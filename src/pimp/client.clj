(ns pimp.client
  (:require [clojure.string :as str]
            [pimp.util :refer [doto-let returning into-by reify+ proxy+
                               keywordize-camel]])
  (:import [org.teleal.cling UpnpService UpnpServiceImpl]
           [org.teleal.cling.controlpoint
              ActionCallback$Default SubscriptionCallback]
           [org.teleal.cling.model.action ActionInvocation]
           [org.teleal.cling.model.message.header
              STAllHeader UDAServiceTypeHeader]
           [org.teleal.cling.model.meta Action Service]
           [org.teleal.cling.model.types UDAServiceId UDAServiceType]
           [org.teleal.cling.registry RegistryListener]))

(defonce ^:private pimp
  (agent {}))

(def ^:private pmc-sid
  "PIMP UPnP service identifier."
  (UDAServiceId. "PartyMediaControl"))

(def ^:private pmc-stype
  "PIMP UPnP service type."
  (UDAServiceType. "PartyMediaControl"))

(defn ^:private upnp-service
  "Return UPnP service implementatino."
  [] (UpnpServiceImpl.))

(defn ^:private event-received
  "Update service value state as PIMP events are received."
  [sub]
  (->> sub .getCurrentValues (map (fn [[k v]] [(keywordize-camel k) (str v)]))
       (into {}) (send pimp assoc :values)))

(defn ^:private pimp-updater
  "Return SubscriptionCallback which updates service value state on events."
  [pmc]
  (proxy+ [SubscriptionCallback] [pmc 600]
    (eventReceived [sub] (event-received sub))))

(defn ^:private device-added
  "Store handle for first PIMP service located."
  [r rd]
  (let [udn (-> rd .getIdentity .getUdn)
        pmc1 (.findService rd pmc-sid)]
    (->> (fn [{:keys [upnp pmc], :as state}]
           (if (or pmc (not upnp))
             state
             (returning (assoc state :pmc pmc1 :udn udn)
               (-> upnp .getControlPoint (.execute (pimp-updater pmc1))))))
         (send pimp))))

(defn ^:private device-removed
  "Release handle for PIMP service when device goes offline."
  [r rd]
  (let [udn1 (-> rd .getIdentity .getUdn)]
    (->> (fn [{:keys [udn], :as state}]
           (if (not= udn udn1)
             state
             (dissoc state :udn :pmc)))
         (send pimp))))

(defn ^:private pimp-detector
  "Return RegistryListener which monitors for PIMP servers."
  []
  (reify+ RegistryListener
    (remoteDeviceAdded [_ r rd] (device-added r rd))
    (remoteDeviceRemoved [_ r rd] (device-removed r rd))))

(defn start
  "Start PIMP client."
  []
  (->> (fn [{:keys [upnp], :as state}]
         (if upnp
           state
           (let [upnp (upnp-service)]
             (returning {:upnp upnp}
               (-> upnp .getRegistry (.addListener (pimp-detector)))
               (-> upnp .getControlPoint
                   (.search (UDAServiceTypeHeader. pmc-stype)))))))
       (send pimp)))

(defn stop
  "Stop PIMP client."
  [] (send pimp (fn [{:keys [upnp]}] (when upnp (.shutdown upnp)))))

(defn ^:private invoke
  "Invoke PIMP service action."
  [action & [item]]
  (let [action (str/capitalize (name action))]
    (->> (fn [{:keys [upnp pmc], :as state}]
           (returning state
             (when (and upnp pmc)
               (-> (doto-let [inv (ActionInvocation. (.getAction pmc action))]
                     (when item (.setInput inv "Item" item)))
                   (ActionCallback$Default. (.getControlPoint upnp))
                   .run))))
         (send-off pimp))))

(defn upvote
  "Upvote current song."
  [] (invoke :upvote "song"))

(defn downvote
  "Downvote current song."
  [] (invoke :downvote "song"))

(defn veto
  "Veto current song."
  [] (invoke :veto))
