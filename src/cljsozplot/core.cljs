(ns ^:figwheel-hooks cljsozplot.core
  (:require
   [oz.core :as oz]
   [testdouble.cljs.csv :as csv]
   [clojure.string :as s]
   [goog.dom :as gdom]
   [alandipert.storage-atom :refer [local-storage]]
   [reagent.core :as reagent :refer [atom]]))

;; 定义测试用的CSV文本，后面可由用户输入
(def sample-csv "time,v,i,t_dc_1,t_dc_2,t_dc_3,t_dc_4,t_m1,t_m2,t_m3,t_sm,t_wf,t_cp1,t_cp2,t_cp3,t_adi,t_pmu,t_cpu,t_ddr_1,t_ddr_2,t_ddr_3,t_ddr_4,t_emmc,note
2020-10-10 14:27,13.2,0.13,32.7,33.7,35.8,36.3,36.5,37.4,37.4,39.3,34.9,31.8,30.9,30.5,36.8,36.9,37.8,36.2,35.1,35.7,35.6,33.2,
2020-10-10 14:30,13.2,0.18,33.9,36.5,37.2,36.5,37.3,38.3,38.1,37.1,35,30.5,30.4,30.3,35.6,36.8,39.4,37.4,37.4,36.1,37.7,36.1,
2020-10-10 14:32,13.2,0.17,34.3,36.3,38.2,37.2,38.2,39.1,38.4,40.4,36,30.4,30.3,30.1,37.7,37.6,40.1,38.2,36.2,37.3,38.7,36.6,
2020-10-10 14:35,13.1,0.17,32.1,38,39.9,38.2,39.2,40.1,39.9,41.1,37.3,32.5,30.9,31.9,38.3,38.2,40.4,38.5,36.4,36.3,38.8,34.4,
2020-10-10 14:38,13.1,0.17,31.8,35.5,38.1,37.9,39,39.5,39.4,41,37.1,34.1,35.8,34.9,37.9,38.6,40.9,37.5,37.3,37.2,39.1,34.5,
2020-10-10 14:40,13.1,0.17,33.1,37.1,38.2,37.7,39,39.5,38.4,40.7,36.7,30.8,30.8,31.4,37.8,38,40.2,38.2,36.5,36.4,38.9,34.2,
2020-10-10 14:43,13.1,0.21,34.1,37.9,39.1,37.8,40.2,40.3,38.1,40.2,36.8,31.5,34.5,33.6,37.4,38,40.8,38.1,37.3,37.3,39.5,35,
2020-10-10 14:45,13.1,0.21,39.3,39.2,39.8,38.9,40,40.9,39.5,40.8,37.3,30.4,32.2,34.4,38.3,38.8,42.1,39.5,38.1,38.9,41.2,32.4,
2020-10-10 14:47,13.2,0.21,41.7,40.4,40.4,39.2,41.6,41.7,40.8,41.8,38,37.1,36.6,36.3,39,39.1,42.3,39.5,38.2,38.3,41.4,35.6,
2020-10-10 14:50,13.2,0.21,39.2,40.8,40.2,40.2,40.1,40.2,39.1,41.8,38.2,34.7,33.3,35.7,39.9,39,42.5,39.3,38,38.2,41.3,36.2,
2020-10-10 14:54,13.2,0.21,37.4,40.6,40.6,40.6,42.4,42.5,41.2,43.1,37.5,34.1,37.5,35.7,39.9,39.7,43.1,39,38,38.9,41,36.8,
2020-10-10 14:57,13.2,0.21,38.3,40.7,41.7,39.8,42.1,43,41.5,43.7,38.5,35.5,37.8,35.5,39.9,39.9,43.4,39.6,38.9,39.8,42,37.9,
2020-10-10 15:02,13.2,0.34,36.4,38.5,38.3,38.3,44,44.9,42.8,47.5,38.5,39,39.7,40.4,40.5,42.4,48.2,40.8,39.8,40.6,43.5,37.1,
2020-10-10 15:05,13.2,0.21,36.9,41.1,40.1,42.2,43,44.9,43.5,45.9,41.1,39.4,40.9,42.4,40.8,42.8,44.3,41.2,40.2,40.1,41.9,38,
2020-10-10 15:09,13.2,0.21,36.6,40.4,37.3,42.7,41.3,42.3,42.9,44.2,38.7,39.7,41.1,40.4,40.9,41.5,43.9,40.7,40.2,41.5,46.5,36.8,
2020-10-10 15:11,13.2,0.35,40,44.4,47.9,43.3,45.4,47.7,45.7,48,41.4,31.7,32.2,38.5,42.6,41.7,46.2,42.2,41.6,43.8,44.8,41.3,
2020-10-10 15:15,13.2,0.21,47.7,47,45.9,37.4,45.9,46.5,44.1,42,40.6,31.2,32,32,40.8,41.6,45.5,42.1,42.6,41.5,45,40.3,
2020-10-10 15:19,13.2,0.21,49.3,47.9,44.5,43,46.1,46.2,46.8,46,39.8,35.7,38.2,36.9,42.5,41.6,45.3,43,41.3,42.3,44.5,38.8,
2020-10-10 15:22,13.2,0.21,40.2,43,43.2,41.9,43.5,44.5,42.2,44.9,39.6,36.9,38.8,37.5,41.6,40.6,44.4,41.9,40.3,42.2,43.9,39,
2020-10-10 15:26,13.2,0.21,36.3,37.1,38.6,39.7,40.7,44.4,43.1,45.2,40.1,38.3,39.8,40.8,42,41.3,45.7,41.3,40.1,40.9,43.5,37.7,
2020-10-10 15:29,13.2,0.21,37.7,37.8,40.7,39.1,43,44.4,42.1,44.1,38.8,35.4,38.8,37.7,40.8,40,43.9,40.3,40.2,40.1,42.2,37.2,
2020-10-10 15:32,13.2,0.21,40.1,41.9,40.6,39.3,44,44,42.5,44.4,38.3,37.8,39,38.9,40.8,40.6,44.2,40,39.9,40.5,42.6,37.6,
")


;; 应用状态
(defonce app-state (local-storage
                    (atom {:sep "," :x-use-index? false :x-as-time? true
                           :x-idx "v, i"
                           :x-idy "t_cpu,t_sm, t_pmu"
                           :idx "time"
                           :idy "v"
                           :csv-field sample-csv})
                    :cljsoz))

(defn get-app-element []
  (gdom/getElement "app"))

(defn- set-val-on-edit! [ky el]
  (swap! app-state assoc
         ky (-> el .-target .-value)
         :error nil))

;; 解析CS文本为map列表
(defn- try-parse-csv! []
  (try
    (let [{:keys [csv-field sep]} @app-state
          [headers & rows] (csv/read-csv csv-field :separator sep)
          headers (map s/trim  headers)
          rows (map-indexed (fn [idx row] (assoc (zipmap headers row) :idx idx)) rows)]
      (swap! app-state assoc :data rows :valid? true))
    (catch :default e
      (throw e)
      (swap! app-state assoc :error (str e) :valid? false :data nil))))

;; 生成ozplot的数据格式
(defn- make-plot! []
  (when (:valid? @app-state)
    (try
      (let [{:keys [idx data idy x-as-time? x-use-index?]} @app-state
            plot {:data {:values data}
                  :encoding {:x {:field (if x-use-index?
                                          "idx"
                                          (str idx))
                                 :type (if (and (not x-use-index?) x-as-time?)
                                         "temporal"
                                         "quantitative")}
                             :y {:field (str idy) :type "quantitative"}
                             :color {:value "black"}}
                  :mark "line"}]
        (swap! app-state assoc :plot plot))
      (catch :default e
        (println "Error parsing data")
        (println e)
        (swap! app-state assoc :error (str e))))))

;; 生成ozplot的数据格式，多行与多列
(defn- make-multi-plot! []
  (when (:valid? @app-state)
    (try
      (let [{:keys [data x-idx x-idy]} @app-state
            plot {:repeat {:column (remove s/blank? (map s/trim (s/split x-idy #",")))
                           :row (remove s/blank? (map s/trim (s/split x-idx #",")))}
                  :spec {:data     {:values data}
                         :mark     "line"
                         :encoding {:x     {:field {:repeat :row} :type "quantitative"}
                                    :y     {:field {:repeat :column} :type "quantitative"}}}}]
        (swap! app-state assoc :multi-plot plot))
      (catch :default e
        (println "Error parsing data")
        (println e)
        (swap! app-state assoc :error (str e))))))

(defn hello-world []
  [:div.ozplot
   [:h1 (:text @app-state)]
   [:h3 "OZPlot"]
   (when-let [err (:error @app-state)]
     [:div.ozplot__error err])
   ;; 获取用户输入CSV文本
   [:div
    [:div.ozplot__textfield
     [:div.ozplot__axislabel "请输入CSV文本"]
     [:textarea {:value (:csv-field @app-state)
                 :on-change (fn [e]
                              (set-val-on-edit! :csv-field e)
                              (try-parse-csv!))}]]]
   ;; 获取用户输入参数
   [:div.ozplot__x
    [:div.ozplot__axislabel "X轴"]
    [:div.ozplot__x--useidx
     [:label {:for "xuseindex"} "使用索引？"]
     [:input {:type :checkbox
              :id "xuseindex"
              :checked (boolean (:x-use-index? @app-state))
              :on-change #(swap! app-state update :x-use-index? not)}]
     (when-not (:x-use-index? @app-state)
       [:div.ozplot__x--options
        [:input {:type :text :value (:idx @app-state) :on-change (partial set-val-on-edit! :idx)}]
        [:label.ozplot__x--label-time {:for "xastime"} "时间？"]
        [:input {:type :checkbox
                 :id "xastime"
                 :checked (boolean (:x-as-time? @app-state))
                 :on-change #(swap! app-state update :x-as-time? not)}]])]]
   [:div.ozplot__y
    [:div.ozplot__axislabel "Y轴"]
    [:input {:type :text :value (:idy @app-state) :on-change (partial set-val-on-edit! :idy)}]
    [:button {:type :button :on-click make-plot!} "显示"]]
   ;; 使用oz绘制图表
   (when-let [plot (:plot @app-state)]
     [:div
      [oz/vega-lite plot]])
   [:h3 "X—Y重复绘制"]
   [:div.ozplot__multi
    [:div.ozplot__multi-axis
     [:div "X轴"]
     [:input {:type :text :placeholder "用逗号分开字段" :value (:x-idx @app-state)
              :on-change (partial set-val-on-edit! :x-idx)}]]
    [:div.ozplot__multi-axis
     [:div "Y轴"]
     [:input {:type :text :placeholder "用逗号分开字段" :value (:x-idy @app-state)
              :on-change (partial set-val-on-edit! :x-idy)}]]
    [:button {:type :button :on-click make-multi-plot!} "显示多图"]]
   (when-let [plot (:multi-plot @app-state)]
     [:div
      [oz/vega-lite plot]])])

(defn mount [el]
  (reagent/render-component [hello-world] el))

(defn mount-app-element []
  (when-let [el (get-app-element)]
    (mount el)))

;; conditionally start your application based on the presence of an "app" element
;; this is particularly helpful for testing this ns without launching the app
(mount-app-element)

(defonce on-load-lazy (delay (try-parse-csv!)))

@on-load-lazy

;; specify reload hook with ^;after-load metadata
(defn ^:after-load on-reload []
  (mount-app-element)
  
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
  )
