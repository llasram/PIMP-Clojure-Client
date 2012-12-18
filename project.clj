(defproject pimp-client "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :repositories [["teleal" "http://teleal.org/m2"]]
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [ring-server "0.2.5"]
                 [compojure/compojure "1.1.3"]
                 [domina/domina "1.0.0"]
                 [org.teleal.cling/cling-core "1.0.5"]
                 [org.teleal.cling/cling-support "1.0.5"]]
  :plugins [[lein-cljsbuild "0.2.9"]
            [lein-ring "0.7.5"]]
  :prep-tasks [["cljsbuild" "once"]]
  :source-paths ["src/clojure"]
  :ring {:handler pimp.client.web/handler}
  :cljsbuild {:builds
              [{:source-path "src/cljs"
                :compiler {:output-to "resources/public/js/pimp.js"
                           :optimizations :whitespace
                           :pretty-print true}}]}
  :main ^:skip-aot pimp.client.web)
