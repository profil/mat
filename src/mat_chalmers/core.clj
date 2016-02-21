(ns mat-chalmers.core
  (:require [mat-chalmers.menu :refer [parse-menu]]
            [clojure.data.json :as json]
            [org.httpkit.server :refer [run-server]])
  (:gen-class))

(defn memo-ttl [ttl f]
  (let [mem (atom {})]
    (fn [& args]
      (let [timestamp (System/currentTimeMillis)
            mem-timestamp (:timestamp @mem)]
        (if (or (nil? mem-timestamp)
                (< ttl (- timestamp mem-timestamp)))
          (let [result (apply f args)]
            (swap! mem assoc :result result :timestamp timestamp)
            result)
          (:result @mem))))))

(def menus
  {"Kårresturangen" "http://intern.chalmerskonferens.se/view/restaurant/karrestaurangen/Veckomeny.rss?today=true"
   "Linsen" "http://intern.chalmerskonferens.se/view/restaurant/linsen/RSS%20Feed.rss?today=true"
   "Einstein" "http://www.butlercatering.se/einstein"
   "Express" "http://intern.chalmerskonferens.se/view/restaurant/express/V%C3%A4nster.rss?today=true"
   "JA-Pripps" "http://intern.chalmerskonferens.se/view/restaurant/j-a-pripps-pub-cafe/RSS%20Feed.rss?today=true"
   ;"Hyllan" "http://intern.chalmerskonferens.se/view/restaurant/hyllan/RSS%20Feed.rss?today=true"
   ;"L's Kitchen" "http://intern.chalmerskonferens.se/view/restaurant/l-s-kitchen/Projektor.rss?today=true"
   ;"L's Resto" "http://intern.chalmerskonferens.se/view/restaurant/l-s-resto/RSS%20Feed.rss?today=true"
   ;"Kokboken" "http://intern.chalmerskonferens.se/view/restaurant/kokboken/RSS%20Feed.rss?today=true"
   })

(defn get-menus [menus]
  (reduce
    (fn [acc [menu url]]
      (assoc acc menu (parse-menu [menu url])))
    {}
    menus))

(def get-data-memo (memo-ttl (* 60 60 1000) get-menus))

#_(defn write-transit [x]
  (let [baos (java.io.ByteArrayOutputStream.)
        w (transit/writer baos :json)
        _ (transit/write w x)
        res (.toString baos)]
    (.reset baos)
    res))

(defn handler [request]
  (condp = (:uri request)
    "/" {:status 200
         :headers {"Content-Type" "application/transit+json; charset=utf-8"
                   "Access-Control-Allow-Origin" "*"}
         :body (json/write-str (get-data-memo menus))}
    "/available-feeds" {:status 200
                        :headers {"Content-Type" "application/transit+json; charset=utf-8"
                                  "Access-Control-Allow-Origin" "*"}
                        :body (json/write-str (keys menus))}
    {:status 404
     :body "Not found"}))

(defn -main [& args]
  (let [ip (get (System/getenv) "OPENSHIFT_CLOJURE_HTTP_IP" "0.0.0.0")
        port (Integer/parseInt (get (System/getenv) "OPENSHIFT_CLOJURE_HTTP_PORT" "8080"))]
    (run-server handler {:ip ip
                         :port port})))
