from flask import Flask, request, \
        render_template, flash, redirect, url_for
from flask_bootstrap import Bootstrap
import models
from models import Post, session as db_session
from views import PostForm

app = Flask(__name__)
app.config.from_mapping(
    SECRET_KEY='asdf',
    SQLALCHEMY_DATABASE_URI=models.SQLALCHEMY_DATABASE_URI,
)
Bootstrap(app)

from flask_sqlalchemy import SQLAlchemy
from flask_migrate import Migrate
migrate = Migrate(app, SQLAlchemy(app))

from flask_admin import Admin
from flask_admin.contrib.sqla import ModelView
admin = Admin(app, name='my admin')
admin.add_view(ModelView(Post, db_session))

@app.route('/', methods=['GET', 'POST'])
def main_form():
    form = PostForm()

    if form.validate_on_submit():
        post = Post()
        form.populate_obj(post)
        db_session.add(post)
        db_session.commit()
        flash('received. thank you')
        redirect(url_for('main_form'))
    
    return render_template(
        'main.html',
        form=form,
        total=models.get_total_posts())

if __name__ == '__main__':
    app.run(debug=True)

