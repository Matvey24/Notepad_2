from com.matvey.perelman.notepad2.list import PythonAPI as api
from com.matvey.perelman.notepad2.list import ElementType

import sys
from io import StringIO

sys.stdout = StringIO()
sys.stderr = StringIO()

def run(string):
    exec(string)
    out = sys.stdout.getvalue()
    err = sys.stderr.getvalue()
    if len(out) != 0:
        api.write(api.path_concat(api.get_path(), 'out.txt'), out)
        sys.stdout.close()
        sys.stdout = StringIO()
    if len(err) != 0:
        api.write(api.path_concat(api.get_path(), 'err.txt'), err)
        sys.stderr.close()
        sys.stderr = StringIO()

#default lib
class api_files:
    def to_py(path):
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
        return l

    def from_py(path, list):
        for el in list:
            p = api.path_concat(path, el['n'])
            if el['t'] == ElementType.FOLDER.ordinal():
                api.mkdir(p)
                api_files.from_py(p, el['f'])
            else:
                api.write(p, el['c'])
                if el['t'] == ElementType.EXECUTABLE.ordinal():
                    api.executable(p, True)
