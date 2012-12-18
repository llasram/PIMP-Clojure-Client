(ns pimp.client.browser
  (:require [cljs.reader :refer [read read-string]]
            [goog.net.XhrIo :as xhr]
            [domina :as d]
            [domina.css :as dc]
            [domina.events :as de]))

(def *refresh-msecs*
  2000)

(def *status*
  {})

(defn upvote!
  [] (xhr/send "/upvote" (fn [_]) "POST"))

(defn downvote!
  [] (xhr/send "/downvote" (fn [_]) "POST"))

(defn veto!
  [] (xhr/send "/veto" (fn [_]) "POST"))

(defn swap-vote
  [this other do! undo!]
  (d/remove-class! other "active")
  (d/add-class! this "active")
  (do!)
  (do!))

(defn undo-vote
  [this other do! undo!]
  (d/remove-class! this "active")
  (undo!))

(defn do-vote
  [this other do! undo!]
  (d/add-class! this "active")
  (do!))

(defn tap-updown
  [this other do! undo!]
  (fn [event]
    (let [this (d/by-id this), other (d/by-id other)]
      (cond (d/has-class? other "active") (swap-vote this other do! undo!)
            (d/has-class? this "active") (undo-vote this other do! undo!)
            :else (do-vote this other do! undo!)))))

(def handle-upvote
  (tap-updown "upvote" "downvote" upvote! downvote!))

(def handle-downvote
  (tap-updown "downvote" "upvote" downvote! upvote!))

(defn handle-veto
  [event]
  (d/remove-class! (d/by-id "upvote") "active")
  (d/remove-class! (d/by-id "downvote") "active")
  (d/add-class! (d/by-id "veto") "active")
  (veto!))

(def status-id
  (juxt :current-song :current-album :current-artist))

(defn handle-status
  [event]
  (let [status (-> event .-target .getResponseText read-string)]
    (when (and status (apply not= (map status-id [status *status*])))
      (set! *status* status)
      (let [{:keys [current-song current-artist album-art-uri]} status]
        (d/remove-class! (dc/sel ".button") "active")
        (d/set-text! (d/by-id "song") current-song)
        (d/set-text! (d/by-id "artist") current-artist)
        (d/set-attr! (d/by-id "art") "src"
                     (if-not (empty? album-art-uri)
                       album-art-uri
                       "img/missing.svg"))))))

(defn check-status
  [] (xhr/send "/status" handle-status))

(defn init
  []
  (when (and js/document (.-getElementById js/document))
    (de/listen! (d/by-id "upvote") :click handle-upvote)
    (de/listen! (d/by-id "downvote") :click handle-downvote)
    (de/listen! (d/by-id "veto") :click handle-veto)
    (check-status)
    (.setInterval js/window check-status *refresh-msecs*)))

(set! (.-onload js/window) init)
