SOURCES := $(shell find src -name '*.cljs')
DOCS_SOURCES := $(shell find docs-src -type f)

.PHONY: clean publish clean_docs

out: build.edn $(SOURCES)
	clojure -m cljs.main -co build.edn -c
	touch out

publish: package.json out
	npm publish

clean:
	rm -rf out

docs: $(DOCS_SOURCES)
	obelix build

clean_docs:
	rm -rf docs
