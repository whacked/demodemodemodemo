(declare-project
  :name "static-sample")

(declare-executable
  :name "static-sample"
  :lflags ["-static"]
  :entry "main.janet")
