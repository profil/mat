(ns mat-chalmers.core
  (:require [reagent.core :refer [render]]
            [reagent.ratom :refer-macros [reaction]]
            [re-frame.core :refer [register-handler
                                   dispatch
                                   register-sub
                                   subscribe]]
            [ajax.core :refer [GET]]
            [cljs.reader :refer [read-string]]))

(defn read-ls []
  (some->> "menu-feeds"
           (js/localStorage.getItem)
           (read-string)))

(defn write-ls [data]
  (some->> data
           (js/localStorage.setItem "menu-feeds")))

(register-handler
  :reload
  (fn [db]
    (let [ls-data (read-ls)
          query (some->> ls-data
                         (str "/?feeds="))]
      (GET (str "http://mat-danielpettersson.rhcloud.com" query)
           {:handler #(dispatch [:result %1])
            :error-handler #(dispatch [:error %1])
            :response-format :json})
      (-> db
          (assoc :menu-feeds ls-data)
          (assoc :status :loading)))))

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

(register-handler
  :edit
  (fn [db]
    (condp = (:edit db)
      :editing (do (write-ls (:menu-feeds db))
                   (assoc db :edit :done))
      (do
        (GET (str "http://mat-danielpettersson.rhcloud.com/available-feeds")
             {:handler #(dispatch [:avail-feeds %1])
              :error-handler #(dispatch [:avail-feeds-error %1])
              :response-format :json})
        (assoc db :edit :loading)))))

(register-handler
  :avail-feeds
  (fn [db [_ res]]
    (-> db
        (assoc :avail-feeds res)
        (assoc :edit :editing))))

(register-handler
  :avail-feeds-error
  (fn [db [_ err]]
    (-> db
        (assoc :avail-feeds-error err)
        (assoc :edit :error))))

(register-handler
  :edit-feed
  (fn [db [_ feed]]
    (if (some #{feed} (:menu-feeds db))
      (update-in db [:menu-feeds] #(vec (remove %2 %1)) #{feed})
      (update-in db [:menu-feeds] conj feed))))


(register-sub
  :status
  (fn [db]
    (reaction (:status @db))))

(register-sub
  :result
  (fn [db]
    (let [res (reaction (:result @db))
          edit (reaction (:edit @db))
          menu-feeds (reaction (:menu-feeds @db))
          avail-feeds (reaction (:avail-feeds @db))]
      (reaction
        (if (= @edit :editing)
          (reduce #(assoc %1 %2 :edit)
                  @res
                  (clojure.set/difference
                    (set @avail-feeds)
                    (set @menu-feeds)))
          (select-keys @res @menu-feeds))))))

(register-sub
  :edit
  (fn [db]
    (reaction (:edit @db))))


(defn menu [m]
  [:ul
   (for [[n dish] m]
     ^{:key dish}
     [:li
      [:h5 (str n)]
      [:p (str dish)]])])

(defn menus [result edit]
  [:section.menus
   (for [[n m] result]
     (if (= m :edit)
       ^{:key n}
       [:article.menu.edit
        (when (= edit :editing)
          {:on-click #(dispatch [:edit-feed n])
           :class "editing"})
        [:h3 (str n)]]
       ^{:key n}
       [:article.menu
        (when (= edit :editing)
          {:on-click #(dispatch [:edit-feed n])
           :class "editing"})
        [:h3 (str n)]
        [menu m]]))])

(defn app []
  (let [status (subscribe [:status])
        result (subscribe [:result])
        edit (subscribe [:edit])]
    (fn []
      [:section
       [:nav {:class (when @status (name @status))}
        [:button.reload
         {:on-click #(dispatch [:reload])}
         (condp = @status
           :error "Error, refresh?"
           :loading "Loading..."
           "Refresh")]
        [:button
         {:on-click #(dispatch [:edit])}
         (condp = @edit
           :error "Error, try again?"
           :loading "Loading feeds"
           :editing "Save"
           "Edit menus")]]
       [menus @result @edit]])))

(defn mount-root []
  (render [app] (js/document.getElementById "app")))

(defn ^:export init []
  (dispatch [:reload])
  (mount-root))
