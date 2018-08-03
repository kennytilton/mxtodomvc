(ns mxtodomvc.matrix
  (:require
    [tiltontec.util.core :as util]

    [tiltontec.cell.core :refer-macros [cF cF+ cFn cF+n cFonce cF1] :refer [cI]]

    [tiltontec.model.core
     ; todo trim
     :refer-macros [with-par]
     :refer [matrix mx-par <mget mset!> mswap!> mxu-find-type] :as md]
    [mxweb.gen
     :refer-macros [div section header h1 footer p ul li span]]
    [mxtodomvc.todo
     :refer [make-todo td-title] :as todo]
    [mxtodomvc.web-components :as webco]
    [mxtodomvc.todo-items-views
     :refer [todo-items-list
             todo-items-dashboard]]
    [mxtodomvc.todo-view
     :refer [todo-list-item]]

    [bide.core :as r]))

;; New for this tag, we have moved the wall-clock to a reusable
;; "web-components" namespace and converted the hard-coded credits
;; to be a reusable component, and placing it in the same home.

(def mxtodo-credits
  ["Double-click a to-do list item to edit it."
   "Created by <a href=\"https://github.com/kennytilton\">Kenneth Tilton</a>."
   "Inspired by <a href=\"https://github.com/tastejs/todomvc/blob/master/app-spec.md\">TodoMVC</a>."])

(defn matrix-build! []
  (reset! md/matrix
    ;; now we provide an optional "type" to support Matrix node space search
    (md/make ::md/todoApp
      ;;
      ;; HTML tag syntax is (<tag> [dom-attribute-map [custom-property map] children*]
      ;;
      :route (cI "All")
      :route-starter (r/start! (r/router [["/" :All]
                                          ["/active" :Active]
                                          ["/completed" :Completed]])
                       {:default     :ignore
                        :on-navigate (fn [route params query]
                                       (when-let [mtx @md/matrix]
                                         (mset!> mtx :route (name route))))})

      :todos (todo/todo-list ["Wash car" "Walk dog" "Do laundry" "Mow lawn"
                              "Wash dog" "Tennis lesson" "Groceries"])

      :mx-dom (cFonce
                (with-par me
                  (section {:class "todoapp" :style "padding:24px"}
                    (webco/wall-clock :date 60000 0 15)
                    (webco/wall-clock :time 1000 0 8)
                    (header {:class "header"}
                      (h1 "todos"))
                    (todo-items-list)
                    (todo-items-dashboard)
                    (webco/app-credits mxtodo-credits)))))))

