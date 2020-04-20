import wtforms
from flask_wtf import FlaskForm
from wtforms_alchemy import ModelForm, model_form_factory
from models import Post, session as db_session

## to use plain ModelForm, you must also inherit Form to enable the CRSF field
## https://stackoverflow.com/questions/33532696/using-csrf-protection-with-wtforms-alchemy#
## i.e.
#class PostForm(ModelForm, Form):
#    pass

BaseModelForm = model_form_factory(FlaskForm)

class CommonBaseForm(BaseModelForm):
    @classmethod
    def get_session(self):
        return db_session

    def __iter__(self):
        appended_fields = []
        for field in super(CommonBaseForm, self).__iter__():
            if isinstance(field, wtforms.fields.SubmitField):
                appended_fields.append(field)
                continue
            yield field
        for appended_field in appended_fields:
            yield appended_field

    submit = wtforms.fields.SubmitField()

class PostForm(CommonBaseForm):
    class Meta:
        model = Post

