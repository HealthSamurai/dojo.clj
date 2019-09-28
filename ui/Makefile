.EXPORT_ALL_VARIABLES:
SHELL = bash
.PHONY: test build

include ../.env

repl:
	clj -A:dev -R:dev -e "(repl)" -r

clean:
	rm -rf target/ .cpcache/

prod:
	clj -m build
