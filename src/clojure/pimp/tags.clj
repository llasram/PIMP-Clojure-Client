(ns pimp.tags
  (:require [clojure.java.io :as io]
            [clojure.xml :as xml])
  (:import [java.net URLEncoder]))

(def ^:dynamic *artist-base-url*
  "http://www.musicbrainz.org/ws/2/artist/")

(defn artist-url
  [artist] (str *artist-base-url* "?query=artist:" (URLEncoder/encode artist)))

(def tag-content
  (comp :content first filter))

(defn artist-tags
  [artist]
  (->>  artist artist-url io/input-stream xml/parse xml-seq
        (tag-content #(and (= :artist (:tag %))
                           (= "100" (get-in % [:attrs :ext:score]))))
        (tag-content #(= :tag-list (:tag %)))
        (mapcat :content) (map #(get-in % [:content 0]))))

(artist-tags "The Decemberists")
;; ("rock" "american" "indie rock" "baroque pop" "indie pop"
;;  "progressive folk" "classic pop and rock")
