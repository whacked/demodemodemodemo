import os
from flask import Flask, request


app = Flask(__name__)

@app.route('/')
def index():
    return '''\
<div>
<h1>hello from iframe</h1>
<script type="application/javascript">
const TOKEN = "%(jwt_token)s";

function postMessage() {
    var xhr = new XMLHttpRequest();
    xhr.open("POST", "http://localhost:%(source_server_port)s/");
    xhr.setRequestHeader("Content-Type", "application/json");
    xhr.setRequestHeader("Authorization", "Bearer " + TOKEN);
    xhr.onreadystatechange = function () {
        if (xhr.readyState == 4) {
           try {
               alert("iframe received a response: " + xhr.responseText);
           } catch (error) {
               alert(error)
               throw Error;
           }
        }
    }
    xhr.send();
}
window.addEventListener("message", function(evt) {
    var message;
    if (evt.origin !== "http://localhost:%(source_server_port)s") {
        message = "failed on origin check: " + evt.origin;
    }
    else {
        message = `${evt.origin} says: ${evt.data}`;
    }
    alert(message);
    postMessage();
})
</script>
</div>
''' % dict(
    source_server_port = os.environ['SERVE_OUTER_PORT'],
    jwt_token = request.values.get('jwt'),
)


app.run('0.0.0.0', port=os.environ['SERVE_INNER_PORT'], debug=True)

