import os
import json
import sqlalchemy
from sqlalchemy import Column, Integer, Text
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.dialects.postgresql.json import JSONB
from sqlalchemy.orm import sessionmaker


connection_string = '{driver}://{user}@{host}:{port}/{name}'.format(
    driver='postgresql',
    user=os.environ["USER"],
    host='localhost',
    port=5432,
    name=os.environ["DATABASE_NAME"],
)
db = sqlalchemy.create_engine(connection_string)
engine = db.connect()
database_meta = sqlalchemy.MetaData(engine)
Session = sessionmaker(bind=engine)
session = Session()

Base = declarative_base(metadata=database_meta)


class MyData(Base):
    __tablename__ = 'my_data'

    id = Column(Integer, primary_key=True)
    user = Column(Text)
    project = Column(Text)
    filename = Column(Text)
    data = Column(JSONB)


database_meta.create_all()


def generate_data():
    import datetime
    from BeanBunny.data import DataStructUtil as DSU

    time_string = str(datetime.datetime.utcnow()).replace(' ', '_').replace(':', '.')
    statement = sqlalchemy.insert(MyData).values(
        user = os.environ['USER'],
        project = 'test',
        filename = 'random-{}'.format(time_string),
        data = DSU.generate_random_datastruct(allow_type=(dict,))
    )
    return engine.execute(statement)


def get_data():
    return sqlalchemy.select([MyData]).execute().fetchall()


def run_server():
    from flask import Flask
    from flask_admin import Admin
    from flask_admin.contrib.sqla import ModelView

    app = Flask(__name__)

    admin = Admin(app)
    admin.add_view(ModelView(MyData, session))

    # now access http://localhost:5000/admin
    app.run()

