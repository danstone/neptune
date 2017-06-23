(ns neptune.client.core
  (:require [goog.net.XhrIo :as xhr]
            [reagent.core :as r]
            [reagent-forms.core :as rforms]
            [paren-soup.core :as psoup]
            [clojure.string :as str]
            [cljs.reader :as cljs-reader]))

(defonce state (r/atom {:counter 0}))

(defn row [label input]
  [:div.row
   [:div.col-md-2 [:label label]]
   [:div.col-md-5 input]])

(defn evaluator-component
  [id]
  (let [doc-state (r/atom {})
        expr-id (str "expr-" id)
        code (get-in @state [:evaluator id :code])
        editor (atom nil)
        form (with-meta
               (fn []
                 [:div {:class "paren-soup" :id expr-id}
                  [:div {:class "instarepl"}]
                  [:div {:class "numbers"}]
                  [:div {:class "content" "contentEditable" "true"}
                   (or code
                     ";; type code here in order to evaluate it e.g\n(+ 1 2)")]])
               {:component-did-mount
                (fn [this]
                  (reset!
                    editor
                    (psoup/init
                      (r/dom-node this)
                      {:compiler-fn
                       (fn [coll receive-fn]
                         (swap! state assoc-in [:evaluator id :code] (str/join "" coll))
                         (xhr/send
                           "http://localhost:8090"
                           (fn [e]
                             (let [s (.getResponseText (.-target e))
                                   [x ns] (cljs-reader/read-string s)]
                               (receive-fn (mapv :instarepl x))
                               (doseq [[kind sym] (keep :control x)]
                                 (swap! doc-state assoc-in [:control sym :kind] kind))
                               (swap! doc-state assoc
                                      :ns ns
                                      :result (:result (peek x)))))
                           "POST"
                           (do
                             (str "{:form [" (str/join " " coll) "], :controls"
                                  (pr-str
                                    (into
                                      {}
                                      (for [[id {:keys [value]}] (:control @doc-state)]
                                        [id value])))
                                  "}"))))})))})
        slider
        (with-meta
          (fn [sym]
            [:input {:type :text
                     :style {:width "100%"}
                     :name (str sym)
                     "data-slider-min" "0"
                     "data-slider-max" "100"
                     "data-slider-step" "1"}])
          {:component-did-mount
           (fn [this]
             (prn "mounted slider")
             (let [node (r/dom-node this)]
               (doto (.slider
                       (js/$ node))
                 (.on "change"
                      (fn [e]
                        (swap!
                          doc-state
                          assoc-in
                          [:control (symbol (.-name (.-target e))) :value]
                          (.slider (js/$ (.-target e)) "getValue"))
                        (psoup/initialize! @editor))))))})]
    (fn []
      [:div {:class "well"}
       [:h2 (str "Cell " id)]
       [:div.row
        [:div.col-md-6
         [form]
         (when-some [controls (seq (:control @doc-state))]
           [:div
            [:h4 "Controls"]
            (for [[sym {:keys [kind value]}] controls]
              [:div {:class "input-group"}
               [:span  {:class "input-group-addon"} sym]
               (case kind
                 :neptune/slider [:div {:class "form-control"} [slider sym]]
                 nil)])])]
        [:div.col-md-6
         (row "ns" (pr-str (:ns @doc-state)))
         (let [{:keys [class out type value]}
               (:result @doc-state)]
           [:div
            (when-not (str/blank? out) (row "out" [:pre out]))
            (row "class" class )
            (row "value" value)])]]])))

(defn simple-component []
  [:div
   [evaluator-component "1"]
   [evaluator-component "2"]])

(r/render [simple-component] (.getElementById js/document "main"))

(swap! state update :counter inc)

(comment

  (.log js/console "hello world")
  (do
    (xhr/send
      "http://localhost:8090"
      (fn [e]
        (prn (.getResponseText (.-target e))))
      "POST"
      "(+ 1 2 3)")
    nil))