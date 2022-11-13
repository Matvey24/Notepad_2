main_code = compile("""def simple_exceptions(func):
    mod_code = compile(f\"\"\"@wraps(func)
def {func.__name__}(*args, **kwargs):
    try:
        return func(*args, **kwargs)
    except RuntimeException as ex:
        string = ex.getMessage()
    raise Exception(string)\"\"\", '*/api.py/simplifier', 'exec')
    dic = {'wraps':wraps, 'func':func, 'RuntimeException':RuntimeException}
    exec(mod_code, dic)    
    return dic[func.__name__]

class Type(enum.Enum):
    FOLDER = 0
    TEXT = 1
    SCRIPT = 2

class Executor:
    def __init__(self, executor, activity, space):
        self.api = API(executor, activity, self)
        self.space = space
        self.glob = None
        self.running = False

    def make_glob(self, name: str, kwargs: dict):
        m = ModuleType(name)
        m.__dict__.update({
            'traceback': traceback,
            'json': json,
            'api': self.api,
            'Type':Type,
            'input': self.api.input,
            'space': self.space,
        })
        m.__dict__.update(kwargs)
        return m

    def execute(self, filename: str, code: str, **kwargs):
        my_stdout = StringIO()
        my_stderr = StringIO()
        saved_stdout = sys.stdout
        saved_stderr = sys.stderr
        sys.stdout = my_stdout
        sys.stderr = my_stderr
        
        self.glob = self.make_glob('__main__', kwargs).__dict__
        
        try:
            self.running = True
            code_object = compile(code, filename, 'exec')
            exec(code_object, self.glob)
        except Exception as ex:
            self.api.toast(repr(ex), True)
            format = traceback.format_exception(*sys.exc_info())
            for i, f in enumerate(format):
                if f.startswith('  File \"/'):
                    try:
                        count = f.count('\\n')
                        if count > 1:
                            f = f[0:f.index('\\n')] + ('!' * (count - 1)) + '\\n'
                        ind = f.rindex('\"')
                        file = f[8:ind]
                        if not self.api.exists(file):
                            continue
                        f2 = f[ind + 8:]
                        line = int(f2[: f2.index(',')])
                        line = self.api.read(file).split('\\n')[line-1].strip()
                        format[i] = f'{f}  ->{line}\\n'
                    except:
                        continue
            print(''.join(format), file=sys.stderr)
        finally:
            self.running = False
        out = my_stdout.getvalue()
        err = my_stderr.getvalue()
        try:
            if len(out) != 0:
                self.api.write('out.txt', out)
            if len(err) != 0:
                self.api.write('err.txt', err)
        except Exception as ex:
            self.api.toast(f'Internal error writing out.txt or err.txt: {repr(ex)}', True)

        sys.stdout = saved_stdout
        sys.stderr = saved_stderr
        my_stdout.close()
        my_stderr.close()

    def from_json(self, st: str, path: str):
        self.api.from_json(st, path)
    
    def __repr__(self):
        return f'Executor(file={self.api.script_path()})'

class API:
    def __init__(self, executor, activity, py_executor):
        self.executor = executor
        self.activity = activity
        self.py_executor = py_executor

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
    
    @simple_exceptions
    def path(self, path='.') -> str:
        return self.executor.getPath(path)

    def script_path(self) -> str:
        return self.executor.getScriptPath()

    def script_name(self) -> str:
        return self.executor.getScriptName()
    
    @simple_exceptions
    def cd(self, dpath: str):
        self.executor.cd(dpath)

    @simple_exceptions
    def view(self, dpath: str):
        self.executor.view(dpath)
    
    def view_path(self)->str:
        return self.executor.getViewPath()
    
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

    def is_file(self, path: str) -> bool:
        type = self.get_type(path)
        return type == Type.SCRIPT or type == Type.TEXT 

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
    def input(self, input_name='input') -> str:
        return self.activity.showInputDialog(input_name)

    @simple_exceptions
    def run(self, fpath: str):
        self.executor.run(fpath)

    @simple_exceptions
    def __reset_params(self, l, fpath):
        self.executor.reset_params(l, fpath)

    def im_port(self, fpath: str, **kwargs) -> ModuleType:
        l = []
        self.__reset_params(l, fpath)
        try:
            code_object = compile(l[2], l[3], 'exec')
            mod = self.py_executor.make_glob(l[4], kwargs)
            exec(code_object, mod.__dict__)
            return mod
        finally:
            self.executor.return_params(l)

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
        if name == '/':
            name = '\\\\'
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

    def to_json(self, path: str, ensure_ascii=False, **kwargs) -> str:
        return json.dumps(self.to_py(path), ensure_ascii=ensure_ascii, **kwargs)

    def from_json(self, d: str, path: str = '.', replace = True):
        d = json.loads(d)
        if replace or not self.exists(self.path_concat(path, d['n'])):
            self.from_py(d, path)
    
    @simple_exceptions
    def add_button(self, name: str, path: str):
        self.activity.registerButton(name, self.path(path))
    
    @simple_exceptions
    def remove_button(self, name:str)->bool:
        return self.activity.unregisterButton(name)

""", "*/api.py", "exec")

from com.matvey.perelman.notepad2.executor import Executor as JAVAExecutor
from java.lang import RuntimeException
from com.matvey.perelman.notepad2 import R
from types import ModuleType
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

exec(main_code)
