from com.matvey.perelman.notepad2.list import ElementType as Type
from com.matvey.perelman.notepad2.executor import Executor as JAVAExecutor
import sys
import traceback
from io import StringIO

import json

def __java_api_make_executor(executor, activity):
    return Executor(executor, activity)

class Executor:
    def __init__(self, executor, activity):
        self.api = API(executor, activity)
        self.api_files = APIFILES(self.api)

    def run_code(self, code: str):
        saved_stdout = StringIO()
        saved_stderr = StringIO()
        sys.stdout = saved_stdout
        sys.stderr = saved_stderr
        glob = {'traceback': traceback, 'json':json, 'api': self.api, 'api_files': self.api_files, 'Type': Type, 'input':self.api.input}
        try:
            exec(code, glob, glob)
        except Exception as ex:
            self.api.toast(repr(ex), True)
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


class API:
    def __init__(self, executor, activity):
        self.executor = executor
        self.activity = activity

    def toast(self, text: str, len = False):
        self.activity.makeToast(text, len)

    def touch(self, tpath: str):
        self.executor.touch(tpath)

    def read(self, fpath: str):
        return self.executor.read(fpath)

    def write(self, fpath: str, content: str):
        self.executor.write(fpath, content)

    def script(self, fpath: str, mode = True):
        self.executor.script(fpath, mode)

    def mkdir(self, dpath: str):
        self.executor.mkdir(dpath)

    def delete(self, path: str):
        return self.executor.delete(path)

    def path(self):
        return self.executor.getPath()

    def script_path(self):
        return self.executor.getScriptPath()

    def script_name(self):
        return self.executor.getScriptName()

    def get_name(self, path: str):
        return self.executor.getName(path)

    def rename(self, path: str, name: str):
        self.executor.rename(path, name)

    @staticmethod
    def path_concat(path1: str, path2: str):
        return JAVAExecutor.path_concat(path1, path2)

    def exists(self, path: str):
        return self.executor.exists(path)

    def is_folder(self, path: str):
        return self.get_type(path) == Type.FOLDER

    def is_script(self, path: str):
        return self.get_type(path) == Type.SCRIPT

    def get_type(self, path: str):
        return self.executor.getType(path)

    def list_files(self, dpath: str):
        return self.executor.listFiles(dpath)

    def move(self, path_cut: str, path_paste: str):
        self.executor.move(path_cut, path_paste)

    def input(self, input_name = 'input'):
        return self.activity.showInputDialog(input_name)

    def run(self, fpath: str):
        self.executor.run(fpath)

    def cd(self, dpath: str):
        self.executor.cd(dpath)



class APIFILES:
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
            raise TypeError(f"from_py requires (str, dict) but taken ({type(path)}, {type(d)})")

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