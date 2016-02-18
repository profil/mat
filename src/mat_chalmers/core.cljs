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
    (assoc db :loading? true)))

(register-handler
  :result
  (fn [db [_ res]]
    (-> db
        (assoc :result res)
        (assoc :loading? false))))

(register-handler
  :error
  (fn [db [_ err]]
    (-> db
        (assoc :error err)
        (assoc :loading? false))))

(register-sub
  :loading
  (fn [db]
    (reaction (:loading @db))))

(register-sub
  :result
  (fn [db]
    (reaction (:result @db))))

(defn app []
  (let [loading (subscribe [:loading])
        result (subscribe [:result])]
    (fn []
      [:div
       (if @loading [:p "loading"] [:p "done"])
       [:button
        {:on-click #(dispatch [:refresh])}
        "reload"]
       [:ul
        (for [[n menu] @result]
          ^{:key menu}
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
