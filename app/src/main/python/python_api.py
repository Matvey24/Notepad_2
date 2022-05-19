from com.matvey.perelman.notepad2.executor import PythonAPI as api
from com.matvey.perelman.notepad2.list import ElementType

import sys
import traceback
from io import StringIO

import json

def __java_api_run(string: str):
    saved_stdout = StringIO()
    saved_stderr = StringIO()
    sys.stdout = saved_stdout
    sys.stderr = saved_stderr
    try:
        exec(string, globals(), globals())
    except Exception as ex:
        api.toast_l(str(ex))
        traceback.print_exc()

    out = saved_stdout.getvalue()
    err = saved_stderr.getvalue()
    if len(out) != 0:
        api.write('out.txt', out)
    if len(err) != 0:
        api.write('err.txt', err)

    saved_stdout.close()
    saved_stderr.close()

def __java_api_from_json(path: str, st: str):
    api_files.from_json(path, st, False)

def input(str = 'input'):
    return api.input(str)

#default lib
class api_files:
    def to_py(path: str):
        if not api.exists(path):
            return None
        if not api.is_folder(path):
            return {'n': api.get_name(path), 't': api.get_type(path).ordinal(), 'c': api.read(path)}

        list = api.list_files(path)
        l = []
        for i in range(list.size()):
            l.append(api_files.to_py(api.path_concat(path, list.get(i).name)))
        return {'n': api.get_name(path), 't': ElementType.FOLDER.ordinal(), 'f': l}

    def from_py(path: str, d: dict):
        if type(d) != dict:
            raise TypeError(f"from_py must get (str, dict) but taken ({type(path)}, {type(d)})")
        f_path = api.path_concat(path, d['n'])
        if d['t'] != ElementType.FOLDER.ordinal():
            api.write(f_path, d['c'])
            if d['t'] == ElementType.SCRIPT.ordinal():
                api.script(f_path, True)
            return

        api.mkdir(f_path)
        for el in d['f']:
            api_files.from_py(f_path, el)

    def to_json(path: str):
        return json.dumps(api_files.to_py(path))

    def from_json(path: str, d: str, replace = True):
        d = json.loads(d)
        if replace or not api.exists(api.path_concat(path, d['n'])):
            api_files.from_py(path, d)
