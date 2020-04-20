import os
import sqlalchemy as sa
from sqlalchemy import create_engine
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker

if os.environ.get('IN_MEMORY'):
    SQLALCHEMY_DATABASE_URI = "sqlite:///:memory:"
else:
    SQLALCHEMY_DATABASE_URI = "sqlite:///test.db"

IN_MEMORY_DATABASE = SQLALCHEMY_DATABASE_URI.endswith(':memory:')

if IN_MEMORY_DATABASE:
    from sqlalchemy.pool import StaticPool
    engine = create_engine(SQLALCHEMY_DATABASE_URI,
            connect_args={'check_same_thread': False},
            poolclass=StaticPool)
else:
    engine = create_engine(SQLALCHEMY_DATABASE_URI)


Base = declarative_base(engine)
Session = sessionmaker(bind=engine)
session = Session()

class Post(Base):
    __tablename__ = 'posts'

    # note BigInteger + autoincrement doesn't work on sqlite
    id = sa.Column(sa.Integer, primary_key=True, autoincrement=True)
    name = sa.Column(sa.Unicode(10), nullable=False)
    greeting = sa.Column(sa.Unicode(20), nullable=True)


def get_total_posts():
    return sa.select([Post]).count().scalar()


if __name__ == '__main__' or IN_MEMORY_DATABASE:
    print('creating databases...')
    Base.metadata.create_all()

