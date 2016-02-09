(ns mat-chalmers.core
  (:require [clojure.xml :refer [parse]]
            [garden.core :refer [css]]
            [garden.stylesheet :refer [at-media]]
            [hiccup.page :refer [html5] :as p]
            [hiccup.util :refer [escape-html]]
            [ring.adapter.jetty :refer [run-jetty]])
  (:gen-class))

(def text (comp (mapcat :content) (filter string?)))

(def children (mapcat :content))

(defn tagp [pred]
  (comp children (filter (comp pred :tag))))

(defn tag= [tag]
  (tagp (partial = tag)))

(defn attrp [attr pred]
  (filter (comp pred (comp attr :attrs))))

(defn attr= [attr value]
  (attrp attr (partial = value)))

(defn extract-menu [url]
  (->> (parse url)
       :content
  (sequence (comp (tag= :item)
                  (tagp #(or (= % :title) (= % :description)))
                  text
                  (map
                    (fn [c]
                      (apply str
                             (->> c
                                  (drop-while #(Character/isWhitespace %))
                                  (take-while (partial not= \@))))))
                  (partition-all 2)))))

(def menus
  {"Kårresturangen" "http://intern.chalmerskonferens.se/view/restaurant/karrestaurangen/Veckomeny.rss?today=true"
   "Express" "http://intern.chalmerskonferens.se/view/restaurant/express/V%C3%A4nster.rss?today=true"
   "JA-Pripps" "http://intern.chalmerskonferens.se/view/restaurant/j-a-pripps-pub-cafe/RSS%20Feed.rss?today=true"
   "Hyllan" "http://intern.chalmerskonferens.se/view/restaurant/hyllan/RSS%20Feed.rss?today=true"
   "Linsen" "http://intern.chalmerskonferens.se/view/restaurant/linsen/RSS%20Feed.rss?today=true"
   "L's Kitchen" "http://intern.chalmerskonferens.se/view/restaurant/l-s-kitchen/Projektor.rss?today=true"
   "L's Resto" "http://intern.chalmerskonferens.se/view/restaurant/l-s-resto/RSS%20Feed.rss?today=true"
   "Kokboken" "http://intern.chalmerskonferens.se/view/restaurant/kokboken/RSS%20Feed.rss?today=true"})

(defn get-data []
  (reduce
    (fn [acc [name url]] (assoc acc name (extract-menu url)))
    {}
    menus))

(defn styles []
  [:style (css {:pretty-print? false
                :vendors ["webkit" "moz"]
                :auto-prefix #{:columns :column-gap}}
               [:body {:background-color "#eee"
                       :color "#4d5152"
                       :box-sizing "border-box"
                       :font-family "'Roboto'"
                       :margin 0
                       :padding 0}]
               [:* {:box-sizing "inherit"}]
               [:#app {:columns "20rem"
                       :column-gap 0
                       :padding "1rem 0.5rem"}]
               [:h3 {:font-weight "normal"
                     :font-size "1.5rem"
                     :margin "0.3rem 0"}]
               [:h5 {:margin "0.3rem 0"}]
               [:p {:font-size "0.8rem"
                    :margin "0.3rem 0"}]
               [:ul {:list-style-type "none"
                     :padding 0
                     :margin 0}]
               [:li {:padding "0.2rem 0"}]
               [:article {:background-color "#fff"
                          :-webkit-column-break-inside "avoid"
                          :page-break-inside "avoid"
                          :break-inside "avoid"
                          :border-radius "3px"
                          :margin "1.5rem 0.5rem"
                          :box-shadow "0 1px 5px rgba(0, 0, 0, 0.16)"
                          :padding "0.5rem 1.5rem"}
                         [:&:first-child {:margin-top 0}] ])])

(defn generate-html []
  (html5 [:head
          [:meta {:http-equiv "Content-type"
                  :content "text/html; charset=utf-8"}]
          [:meta {:name "viewport"
                  :content "width=device-width, initial-scale=1"}]
          (p/include-css "https://fonts.googleapis.com/css?family=Roboto")
          (styles)
          [:title "Lunch - Chalmers"]]
                [:body
                 [:section#app
                  (map
                    (fn [[name menu]]
                      [:article
                       [:h3 (escape-html name)]
                       [:ul
                        (map
                          (fn [[title desc]]
                            [:li
                             [:h5 (escape-html title)]
                             [:p (escape-html desc)]])
                          menu)]])
                    (get-data))]]))

(defn handler [atom-data]
  (fn [request]
    (if (= (:uri request) "/")
      (let [{:keys [last-modified data]} @atom-data]
        (when (> (- (System/currentTimeMillis) last-modified) 3600000)
          (reset! atom-data {:data (generate-html)
                             :last-modified (System/currentTimeMillis)}))
        {:status 200
         :headers {"Content-Type" "text/html"}
         :body data})
      {:status 404
       :body "Not found"})))

(defn -main [& args]
  (let [ip (get (System/getenv) "OPENSHIFT_CLOJURE_HTTP_IP" "0.0.0.0")
        port (Integer/parseInt (get (System/getenv) "OPENSHIFT_CLOJURE_HTTP_PORT" "8080"))
        atom-data (atom {:data (generate-html)
                         :last-modified (System/currentTimeMillis)})]
    (run-jetty (handler atom-data) {:host ip
                                    :port port})))
