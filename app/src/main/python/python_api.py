from com.matvey.perelman.notepad2.executor import Executor as JAVAExecutor
from java.lang import RuntimeException
import sys
import traceback
import enum
from io import StringIO

import json
from functools import wraps



def __java_api_make_dict():
    return {}

def __java_api_make_executor(executor, activity, space):
    return Executor(executor, activity, space)

def simple_exceptions(func):
    @wraps(func)
    def wrapper(*args, **kwargs):
        try:
            return func(*args, **kwargs)
        except RuntimeException as ex:
            string = ex.getMessage()
        raise Exception(string)
    
    return wrapper


class Type(enum.Enum):
    FOLDER = 0
    TEXT = 1
    SCRIPT = 2


class Executor:
    def __init__(self, executor, activity, space):
        self.api = API(executor, activity)
        self.space = space

    def run_code(self, filename: str, code: str):
        saved_stdout = StringIO()
        saved_stderr = StringIO()
        sys.stdout = saved_stdout
        sys.stderr = saved_stderr
        glob = {'traceback': traceback, 'json': json, 'api': self.api, 'Type':Type, 'input': self.api.input, 'space': self.space}
        try:
            code_object = compile(code, filename, 'exec')
            exec(code_object, glob)
        except Exception as ex:
            self.api.toast(repr(ex), True)
            traceback.print_exc()

        out = saved_stdout.getvalue()
        err = saved_stderr.getvalue()
        try:
            if len(out) != 0:
                self.api.write('out.txt', out)
            if len(err) != 0:
                self.api.write('err.txt', err)
        except Exception as ex:
            self.api.toast(f'Internal error writing out.txt or err.txt: {repr(ex)}', True)

        saved_stdout.close()
        saved_stderr.close()

    def from_json(self, st: str, path: str):
        self.api.from_json(st, path)


class API:
    def __init__(self, executor, activity):
        self.executor = executor
        self.activity = activity

    def toast(self, text: str, len: bool = False):
        self.activity.makeToast(text, len)

    @simple_exceptions
    def touch(self, tpath: str):
        self.executor.touch(tpath)

    @simple_exceptions
    def read(self, fpath: str) -> str:
        return self.executor.read(fpath)

    @simple_exceptions
    def write(self, fpath: str, content: str):
        self.executor.write(fpath, content)

    @simple_exceptions
    def script(self, fpath: str, mode :bool = True):
        self.executor.script(fpath, mode)

    @simple_exceptions
    def mkdir(self, dpath: str):
        self.executor.mkdir(dpath)

    @simple_exceptions
    def delete(self, path: str) -> bool:
        return self.executor.delete(path)

    def path(self) -> str:
        return self.executor.getPath()

    def script_path(self) -> str:
        return self.executor.getScriptPath()

    def script_name(self) -> str:
        return self.executor.getScriptName()

    @simple_exceptions
    def get_name(self, path: str) -> str:
        return self.executor.getName(path)

    @simple_exceptions
    def rename(self, path: str, name: str):
        self.executor.rename(path, name)

    @staticmethod
    def path_concat(path1: str, path2: str) -> str:
        return JAVAExecutor.path_concat(path1, path2)

    @simple_exceptions
    def exists(self, path: str) -> bool:
        return self.executor.exists(path)

    def is_folder(self, path: str) -> bool:
        return self.get_type(path) == Type.FOLDER

    def is_script(self, path: str) -> bool:
        return self.get_type(path) == Type.SCRIPT

    @simple_exceptions
    def get_type(self, path: str) -> Type:
        return Type(self.executor.getType(path).ordinal())

    @simple_exceptions
    def list_files(self, dpath: str) -> list:
        cursor = self.executor.listFiles(dpath)
        l = []
        for i in range(cursor.getCount()):
            cursor.moveToPosition(i)
            l.append({'name': cursor.getString(1), 'type': Type(cursor.getInt(2)), 'content': cursor.getString(3)})

        cursor.close()
        return l

    @simple_exceptions
    def move(self, path_cut: str, path_paste: str):
        self.executor.move(path_cut, path_paste)

    @simple_exceptions
    def input(self, input_name = 'input') -> str:
        return self.activity.showInputDialog(input_name)

    @simple_exceptions
    def run(self, fpath: str):
        self.executor.run(fpath)

    @simple_exceptions
    def cd(self, dpath: str):
        self.executor.cd(dpath)

    @simple_exceptions
    def view(self, dpath: str):
        self.executor.view(dpath)

    def to_py(self, path: str) -> dict:
        if not self.exists(path):
            raise FileNotFoundError(path)
        p = self.path()
        try:
            name = self.get_name(path)
            self.cd(self.path_concat(path, ".."))
            d = self.__to_py_req(name, self.get_type(name).value)
        finally:
            self.cd(p)

        return d

    def __to_py_req(self, name: str, type: int) -> dict:
        if not self.is_folder(name):
            return {'n': name, 't': type, 'c': self.read(name)}

        self.cd(name)
        l = self.list_files('.')
        for i in range(len(l)):
            l[i] = self.__to_py_req(l[i]['name'], l[i]['type'].value)
        self.cd('..')
        return {'n': name, 't': type, 'f': l}

    def from_py(self, d: dict, path: str = '.', replace = True):
        if type(d) != dict:
            raise TypeError(f"from_py requires (dict, str, bool) but taken ({type(path)}, {type(d)}, {type(replace)})")

        if not replace and self.exists(self.path_concat(path, d['n'])):
            return

        p = self.path()
        try:
            self.cd(path)
            self.__from_py_req(d)
        finally:
            self.cd(p)

    def __from_py_req(self, d: dict):
        if d['t'] != Type.FOLDER.value:
            self.script(d['n'], d['t'] is Type.SCRIPT.value)
            self.write(d['n'], d['c'])
            return

        self.cd(d['n'])
        for el in d['f']:
            self.__from_py_req(el)
        self.cd('..')

    def to_json(self, path: str) -> str:
        return json.dumps(self.to_py(path))

    def from_json(self, d: str, path: str = '.', replace = True):
        d = json.loads(d)
        if replace or not self.exists(self.path_concat(path, d['n'])):
            self.from_py(d, path)
