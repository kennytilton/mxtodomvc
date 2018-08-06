(ns mxtodomvc.todo-view
  (:require
    [goog.dom :as dom]
    [goog.dom.classlist :as classlist]
    [goog.dom.forms :as form]

    [tiltontec.util.core :as util]
    [tiltontec.cell.base :refer [unbound]]
    [tiltontec.cell.observer :refer-macros [fn-obs]]
    [tiltontec.cell.core :refer-macros [cF cF+]]
    [tiltontec.cell.evaluate :refer [not-to-be]]

    [tiltontec.model.core :refer [matrix mx-par <mget mset!> ;; mswap!>
                                  ;;fget mxi-find mxu-find-type
                                  ] :as md]

    [mxxhr.core
     :refer [make-xhr xhr-response]]

    [mxweb.gen
     :refer-macros [label li div input button span i]]
    [mxweb.html :as mxweb]

    [mxtodomvc.todo
     :refer [td-title td-created
             td-completed td-delete!
             td-id td-toggle-completed!]]
    [cljs.pprint :as pp]
    [clojure.string :as str]))

;;; -----------------------------------------------------------
;;; --- adverse events ----------------------------------------


(def ae-by-brand "https://api.fda.gov/drug/event.json?search=patient.drug.openfda.brand_name:~(~a~)&limit=3")

(defn ae-brand-uri [todo]
  (pp/cl-format nil ae-by-brand
    (js/encodeURIComponent (td-title todo))))

(defn xhr-scavenge [xhr]
  (when-not (or (= xhr unbound) (nil? xhr))
    (not-to-be xhr)))

(defn de-whitespace [s]
  (str/replace s #"\s" ""))

(defn ae-checker-style-formula
  "Just breaking out the code, illustrating an incidental coding convenience"
  []
  (cF (str "font-size:36px"

        ";display:"
        (case (<mget me :aes?)
          :no "none"
          "block")

        ";color:"
        (case (<mget me :aes?)
          :undecided "gray"
          :yes "red"
          :no "green"
          "white"))))

(defn adverse-event-checker [todo]
  (i
    {:class "aes material-icons"
     ;;:title "Click to see some AE counts"
     :style (ae-checker-style-formula)
     :onclick #(js/alert "Feature to display AEs not yet implemented")}

    {:lookup   (cF+ [:obs (fn-obs (xhr-scavenge old))]
                 (make-xhr (pp/cl-format nil ae-by-brand
                             (js/encodeURIComponent
                               (de-whitespace (td-title todo))))
                   {:name name :send? true
                    :fake-delay (+ 500 (rand-int 2000))}))
     :response (cF (when-let [xhr (<mget me :lookup)]
                     (xhr-response xhr)))
     :aes?      (cF (if-let [r (<mget me :response)]
                      (if (= 200 (:status r)) :yes :no)
                      :undecided))}
    "warning"))

(defn todo-edit [e todo edit-commited?]
  (let [edt-dom (.-target e)
        li-dom (dom/getAncestorByTagNameAndClass edt-dom "li")]

    (when (classlist/contains li-dom "editing")
      (let [title (str/trim (form/getValue edt-dom))
            stop-editing #(classlist/remove li-dom "editing")]
        (cond
          edit-commited?
          (do
            (stop-editing)                                  ;; has to go first cuz a blur event will sneak in
            (if (= title "")
              (td-delete! todo)
              (mset!> todo :title title)))

          (= (.-key e) "Escape")
          ;; this could leave the input field with mid-edit garbage, but
          ;; that gets initialized correctly when starting editing
          (stop-editing))))))

(defn todo-list-item [todo]
  ;; the structure below, and importantly its CSS, was authored
  ;; by the developers of the TodoMVC exercise. Nice!

  ;; Nothing new to note here, except
  (li
    {:class (cF (when (td-completed todo)
                  "completed"))}

    {:todo todo}

    (div {:class "view"}
      (input {:class       "toggle"
              ;; namespaced :type is for HTML attribute
              ::mxweb/type "checkbox"
              :checked     (cF
                             ;; completed is not a boolean, it is
                             ;; a timestamp that is nil? until the to-do is completed
                             (not (nil? (td-completed todo))))

              ;; td-toggle-completed! expands to an mset!> of the JS epoch or nil
              :onclick     #(td-toggle-completed! todo)})

      (label {:ondblclick #(let [li-dom (dom/getAncestorByTagNameAndClass
                                          (.-target %) "li")
                                 edt-dom (dom/getElementByClass
                                           "edit" li-dom)]
                             (classlist/add li-dom "editing")
                             (mxweb/input-editing-start edt-dom (td-title todo)))}
        (td-title todo))

      (adverse-event-checker todo)

      (button {:class   "destroy"
               ;; we actually have an td-delete! to hide the action, but
               ;; this is a tutorial so let's show the action and use mset!.
               ;; btw, yes, we extend here the spec to support logical deletion
               :onclick #(md/mset!> todo :deleted (util/now))}))

    (input {:class     "edit"
            :onblur    #(todo-edit % todo true)
            :onkeydown #(todo-edit % todo (= (.-key %) "Enter"))})))