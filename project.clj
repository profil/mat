(defproject mat-chalmers "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.228"]
                 [cljs-ajax "0.5.3"]
                 [re-frame "0.7.0-alpha-2"]
                 [ring/ring-core "1.4.0"]
                 [http-kit "2.1.18"]
                 [org.clojure/data.xml "0.0.8"]
                 [org.clojure/data.json "0.2.6"]]
  :plugins [[lein-ring "0.9.7"]
            [lein-figwheel "0.5.0-6"]
            [lein-cljsbuild "1.1.2"]]
  :ring {:handler mat-chalmers.core/handler
         :auto-refresh? true}
  :main ^:skip-aot mat-chalmers.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}}
  :figwheel {:css-dirs ["resources/public/css"]}
  :cljsbuild {:builds [{:id "dev"
                        :source-paths ["src"]
                        :figwheel {:on-jsload "mat-chalmers.core/mount-root"}
                        :compiler {:main mat-chalmers.core
                                   :asset-path "js/out"
                                   :output-to "resources/public/js/main.js"
                                   :output-dir "resources/public/js/out"}}
                       {:id "min"
                        :source-paths ["src"]
                        :compiler {:main mat-chalmers.core
                                   :output-to "resources/public/js/main.js"
                                   :optimizations :advanced
                                   :pretty-print false}}]})
