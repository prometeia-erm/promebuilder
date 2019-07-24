import requests
import json
import getpass
import socket
import os

class TestReporter(object):

    def __init__(self, slackurl):
        self.slackurl = slackurl
        self.results = {}

    def send(self):
        if not self.results:
            return
        rep = self.gen_text_report()
        if self.slackurl:
            requests.post(self.slackurl, headers = {'Content-type': 'application/json'},
                          data=json.dumps({"text": rep}))
        print("\n{}".format(rep))

    def gen_text_report(self):
        payload = "{} _{}@{}_ ".format(os.path.basename(os.getcwd()), getpass.getuser(), socket.gethostname())
        try:
            payload += "{} ".format(open('.git/HEAD').read().strip())
        except IOError:
            pass
        tot = len(self.results)
        ok = sum(filter(lambda x: int(x), self.results.values()))
        payload += "{}/{} {}".format(ok, tot, ":meow_knife:" if ok < tot else ":+1:")
        return payload

    def add_report(self, report):
        if not report.skipped:
            self.results[report.nodeid] = report.passed
