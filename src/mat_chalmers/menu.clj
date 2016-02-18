(ns mat-chalmers.menu
  (:require [clojure.zip :as zip]
            [clojure.data.xml :refer [parse]]
            [clojure.string :as string]))

(defn swedish-weekday []
  (let [weekdays ["Söndag" "Måndag" "Tisdag"
                  "Onsdag" "Torsdag" "Fredag"
                  "Lördag"]]
    (weekdays (dec (.get (java.util.Calendar/getInstance)
                         java.util.Calendar/DAY_OF_WEEK)))))

(defn whitespace? [c]
  (Character/isWhitespace c))

(defmulti parse-menu first)

(defmethod parse-menu "Einstein" [[menu url]]
  (with-open [xin (clojure.java.io/input-stream url :encoding "UTF-8")]
    (let [loc (-> xin
                  parse
                  zip/xml-zip)
          [day] (sequence
                  (comp (take-while (complement zip/end?))
                        (filter #(and (= "field-day"
                                         (get-in (zip/node %) [:attrs :class]))
                                      (= (swedish-weekday) (-> %
                                                               zip/down 
                                                               zip/down 
                                                               zip/node))))
                        (take 1))
                  (iterate zip/next loc))
          res (sequence
                (comp (take-while (complement zip/end?))
                      (filter #(= :p (:tag (zip/node %))))
                      (map #(string/replace (zip/node (zip/down %))
                                            "\u00A0" ""))
                      (filter (complement string/blank?))
                      (map #(list "Lunch" %)))
                (iterate zip/next (zip/xml-zip (zip/node day))))]
      res)))

(defmethod parse-menu :default [[menu url]]
  (with-open [xin (clojure.java.io/input-stream url :encoding "UTF-8")]
    (let [loc (-> xin
                  parse
                  zip/xml-zip)
          res (sequence
                (comp (take-while (complement zip/end?))
                      (filter #(= :item (:tag (zip/node %))))
                      (map #(list (zip/node (zip/down (zip/down %)))
                                  (->> %
                                       zip/down
                                       zip/next
                                       zip/next
                                       zip/next
                                       zip/next
                                       zip/down
                                       zip/node
                                       (drop-while whitespace?)
                                       (take-while (partial not= \@))
                                       (apply str)))))
                (iterate zip/next loc))]
      res)))
