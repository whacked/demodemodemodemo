import os
import jwt
import datetime
from flask import Flask, request, make_response


SECRET_HIDDEN_FROM_CLIENT = 'my-secretive-secret'
app = Flask(__name__)


def add_cors_headers(response):
    response.headers.update({
        'Access-Control-Allow-Origin': 'http://localhost:{}'.format(os.environ['SERVE_INNER_PORT']),
        'Access-Control-Allow-Headers': 'content-type,{}'.format(
            'Authorization',
        )
    })
    return response

@app.route('/', methods=['GET', 'POST', 'OPTIONS'])
def index():
    if request.method == 'OPTIONS':
        response = make_response()
        return add_cors_headers(response)

    elif request.method == 'POST':
        jwt_token = request.headers['authorization'].replace('Bearer ', '')
        # just extract the payload
        raw_data = jwt.decode(jwt_token, verify=False)
        # this request should be matched against something only the server knows
        try:
            verified_data = jwt.decode(jwt_token, SECRET_HIDDEN_FROM_CLIENT)
            response_message = verified_data.get('message')
        except jwt.exceptions.ExpiredSignatureError as e:
            response_message = 'FAILED! you waited too long'
        response = make_response(
            'outer received: {}'.format(response_message),
            200)
        return add_cors_headers(response)

    return '''\
<iframe src="http://localhost:%(target_server_port)s?jwt=%(jwt_token)s"></iframe>
<script type="application/javascript">
window.onload = function() {
    let win = document.getElementsByTagName('iframe')[0].contentWindow;
    let messageToIframe = 'message from host to iframe';
    let targetOrigin = 'http://localhost:%(target_server_port)s';
    win.postMessage(messageToIframe, targetOrigin);

    function handleMessage(e) {
        if ( e.origin === 'http://localhost:%(target_server_port)s' ) {
            alert('host received message from iframe: ' + e.data);
            // send message back to host
            e.source.postMessage('iframe received: "' + e.data + '"', e.origin);
        }
    }
    window.addEventListener('message', handleMessage, false);
}
</script>
''' % dict(
    jwt_token = str(jwt.encode({
        'exp': datetime.datetime.utcnow() + datetime.timedelta(seconds=5),
        'message': 'Success!',
    }, SECRET_HIDDEN_FROM_CLIENT), 'utf-8'),
    source_server_port = os.environ['SERVE_OUTER_PORT'],
    target_server_port = os.environ['SERVE_INNER_PORT'],
)


app.run('0.0.0.0', port=os.environ['SERVE_OUTER_PORT'], debug=True)

