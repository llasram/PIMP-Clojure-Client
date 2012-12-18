(ns pimp.client.web
  (:require [clojure.string :as str]
            [ring.server.standalone :as sa]
            [compojure.core :refer [defroutes GET POST]]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [hiccup.core :refer [html]]
            [pimp.client :as pimp]))

(def page
  [:html {:lang "en"}
   [:head
    [:meta {:name "viewport"
            :content (->> ["width=device-width"
                           "initial-scale=1.0"
                           "maximum-scale=1.0"
                           "user-scalable=no"]
                          (str/join ", "))}]
    [:meta {:charset "utf-8"}]
    [:link {:rel "stylesheet" :type "text/css"
            :href "http://fonts.googleapis.com/css?family=Exo"}]
    [:link {:rel "stylesheet" :type "text/css" :href "css/pimp.css"}]
    [:title "Party Interface Media Player"]]
   [:body
    [:div#container
     [:div#album
      [:img#art {:src "img/missing.svg"}]
      [:div#title
       [:div#song "Song"]
       [:div#artist "Artist"]]]
     [:div#buttons
      [:img#upvote.button {:src "img/upvote.png"}]
      [:img#veto.button {:src "img/veto.png"}]
      [:img#downvote.button {:src "img/downvote.png"}]]]
    [:script {:src "js/pimp.js"}]]])

(defroutes app-routes
  (GET "/" [] (str "<!doctype html>\n" (html page)))
  (GET "/status" [] (prn-str (pimp/status)))
  (POST "/upvote" [] (pimp/upvote!))
  (POST "/downvote" [] (pimp/downvote!))
  (POST "/veto" [] (pimp/veto!))
  (route/resources "/")
  (route/not-found "Page not found"))

(def handler
  (handler/site app-routes))

(defn -main
  [& args]
  (let [[port] args, port (or port 3000)]
    (sa/serve
     #'handler
     {:port port, :join? true, :open-browser? false,
      :init pimp/start, :destroy pimp/stop})))

(comment

  (def server
    (sa/serve #'handler {:auto-reload? false}))
  (.stop server)

  )
