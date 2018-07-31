(ns mxtodomvc.todo-view
  (:require
    [tiltontec.cell.core :refer-macros [cF]]
    [mxweb.gen
     :refer-macros [label li div input button]]
    [mxweb.html :as mxweb]
    [mxtodomvc.todo
     :refer [td-title td-created
             td-completed td-delete!
             td-id td-toggle-completed!]]))

(defn todo-list-item [todo]
  (li {:class   (cF (when (td-completed todo) "completed"))}
    {:todo      todo}

    (div {:class "view"}
      (input {:class   "toggle" ::mxweb/type "checkbox"
              :checked (cF (not (nil? (td-completed todo))))
              :onclick #(td-toggle-completed! todo)})

      (label (td-title todo))

      (button {:class   "destroy"
               :onclick #(td-delete! todo)}))))