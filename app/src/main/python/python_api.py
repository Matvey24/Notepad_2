from com.matvey.perelman.notepad2.list import ElementType as Type

import sys
import traceback
from io import StringIO

import json

def __java_api_make_executor(api):
    return executor(api)

class executor:
    def __init__(self, api):
        self.api = api
        self.api_files = api_files(api)

    def run_code(self, code: str):
        saved_stdout = StringIO()
        saved_stderr = StringIO()
        sys.stdout = saved_stdout
        sys.stderr = saved_stderr
        glob = {'sys': sys, 'traceback': traceback, 'json':json, 'api': self.api, 'api_files': self.api_files, 'Type': Type, 'input':self.input}
        try:
            exec(code, glob, glob)
        except Exception as ex:
            self.api.toast_l(repr(ex))
            traceback.print_exc()

        out = saved_stdout.getvalue()
        err = saved_stderr.getvalue()
        if len(out) != 0:
            self.api.write('out.txt', out)
        if len(err) != 0:
            self.api.write('err.txt', err)

        saved_stdout.close()
        saved_stderr.close()

    def from_json(self, path: str, st: str):
        self.api_files.from_json(path, st)

    def input(self, str = 'input'):
        return self.api.input(str)

class api_files:
    def __init__(self, api):
        self.api = api

    def to_py(self, path: str):
        if not self.api.exists(path):
            return None
        if not self.api.is_folder(path):
            return {'n': self.api.get_name(path), 't': self.api.get_type(path).ordinal(), 'c': self.api.read(path)}

        list = self.api.list_files(path)
        l = []
        for i in range(list.size()):
            l.append(self.to_py(self.api.path_concat(path, list.get(i).name)))
        return {'n': self.api.get_name(path), 't': Type.FOLDER.ordinal(), 'f': l}

    def from_py(self, path: str, d: dict):
        if type(d) != dict:
            raise TypeError(f"from_py must get (str, dict) but taken ({type(path)}, {type(d)})")
        f_path = self.api.path_concat(path, d['n'])
        if d['t'] != Type.FOLDER.ordinal():
            self.api.write(f_path, d['c'])
            if d['t'] == Type.SCRIPT.ordinal():
                self.api.script(f_path, True)
            return

        self.api.mkdir(f_path)
        for el in d['f']:
            self.from_py(f_path, el)

    def to_json(self, path: str):
        return json.dumps(self.to_py(path))

    def from_json(self, path: str, d: str, replace = True):
        d = json.loads(d)
        if replace or not self.api.exists(self.api.path_concat(path, d['n'])):
            self.from_py(path, d)