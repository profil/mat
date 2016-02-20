(ns mat-chalmers.core
  (:require [reagent.core :refer [render]]
            [reagent.ratom :refer-macros [reaction]]
            [re-frame.core :refer [register-handler
                                   dispatch
                                   register-sub
                                   subscribe]]
            [ajax.core :refer [GET]]))

(register-handler
  :refresh
  (fn [db]
    (GET "http://mat-danielpettersson.rhcloud.com"
         {:handler #(dispatch [:result %1])
          :error-handler #(dispatch [:error %1])
          :response-format :json})
    (assoc db :status :loading)))

(register-handler
  :result
  (fn [db [_ res]]
    (-> db
        (assoc :result res)
        (assoc :status :done))))

(register-handler
  :error
  (fn [db [_ err]]
    (-> db
        (assoc :error err)
        (assoc :status :error))))

(register-sub
  :status
  (fn [db]
    (reaction (:status @db))))

(register-sub
  :result
  (fn [db]
    (reaction (:result @db))))

(defn app []
  (let [status (subscribe [:status])
        result (subscribe [:result])]
    (fn []
      [:div
       [:p (str @status)]
       [:button
        {:on-click #(dispatch [:refresh])}
        "reload"]
       [:ul
        (for [[n menu] @result]
          ^{:key n}
          [:li
           [:p (str n)]
           (for [[n dish] menu]
             ^{:key dish}
             [:div
              [:p (str n)]
              [:p (str dish)]])])]])))

(defn mount-root []
  (render [app] (js/document.getElementById "app")))

(defn ^:export init [] 
  (dispatch [:refresh])
  (mount-root))
