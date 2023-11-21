classes = Server.class Client.class Terminal.class TermServer.class TermClient.class

$(classes): %.class: %.java
	javac $<
        # $(CC) -c $(CFLAGS) $< -o $@

all: compile

compile:
	javac *.java

up: Server.class Client.class
	docker compose -f compose.yaml --profile routers --profile endpoints up -d

down:
	docker compose -f compose.yaml --profile routers --profile endpoints down -t 1

upterm: TermServer.class TermClient.class Terminal.class
	-@echo "** Start X11 server before starting this experiment **"
	docker compose -f term.compose.yaml --profile routers --profile endpoints up -d

downterm:
	docker compose -f term.compose.yaml --profile routers --profile endpoints down -t 1

clean:
	-@echo "Cleaning..."
	-@rm -rf *.class
	-@rm -rf .DS_Store

veryclean: clean down downterm
	-@echo "Scrubbing images..."
	-@docker image rm jvimage
	-@docker image prune -fa
