from com.matvey.perelman.notepad2.list import PythonAPI as api
from com.matvey.perelman.notepad2.list import ElementType

import sys
from io import StringIO

import json

def __java_api_run(string: str):
    saved_stdout = StringIO()
    saved_stderr = StringIO()
    sys.stdout = saved_stdout
    sys.stderr = saved_stderr
    exec(string)
    out = saved_stdout.getvalue()
    err = saved_stderr.getvalue()
    if len(out) != 0:
        api.write(api.path_concat(api.get_path(), 'out.txt'), out)
    if len(err) != 0:
        api.write(api.path_concat(api.get_path(), 'err.txt'), err)

    saved_stdout.close()
    saved_stderr.close()

def __java_api_from_json(path: str, st: str):
    api_files.from_json(path, st, False)

def __java_api_copying_move(path_from: str, path_to: str):
    obj = api_files.to_py(path_from)
    api_files.from_py(path_to, obj)
    api.delete(path_from)

#default lib
class api_files:
    def to_py(path: str):
        if not api.exists(path):
            return None
        if not api.is_dir(path):
            return {'n': api.get_name(path), 't': (ElementType.EXECUTABLE.ordinal() if api.is_executable(path) else ElementType.TEXT.ordinal()), 'c': api.read(path)}

        list = api.list_files(path)
        l = []
        for i in range(list.size()):
            el = list.get(i)
            if el.type == ElementType.FOLDER:
                d = {'n': el.name, 't': el.type.ordinal(), 'f': api_files.to_py(api.path_concat(path, el.name))}
                l.append(d)
            else:
                d = {'n': el.name, 't': el.type.ordinal(), 'c': el.content}
                l.append(d)
        return {'n': api.get_name(path), 't': ElementType.FOLDER.ordinal(), 'f': l}

    def from_py(path: str, d: dict):
        if type(d) != dict:
            raise TypeError(f"from_py must get (str, dict) but taken ({type(path)}, {type(d)})")
        f_path = api.path_concat(path, d['n'])
        if d['t'] != ElementType.FOLDER.ordinal():
            api.write(f_path, d['c'])
            if d['t'] == ElementType.EXECUTABLE.ordinal():
                api.executable(f_path, True)
            return

        api.mkdir(f_path)
        for el in d['f']:
            api_files.from_py(f_path, el)

    def to_json(path: str):
        return json.dumps(api_filer.to_py(path))

    def from_json(path: str, d: str, replace = True):
        if not replace and api.exists(path):
            return
        api_files.from_py(path, json.loads(d))
