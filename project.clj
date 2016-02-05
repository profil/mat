(defproject mat-chalmers "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [hiccup "1.0.5"]
                 [garden "1.3.0"]
                 [ring/ring-core "1.4.0"]
                 [ring/ring-jetty-adapter "1.4.0"]]
  :plugins [[lein-ring "0.9.7"]]
  :ring {:handler mat-chalmers.core/handler
         :auto-refresh? true}
  :main ^:skip-aot mat-chalmers.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
