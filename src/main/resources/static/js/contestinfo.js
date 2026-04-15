var code_editor;

// 代码模板定义
var codeTemplates = {
    "c": `#include <stdio.h>

int main() {
    // 从标准输入读取数据
    // int n;
    // scanf("%d", &n);
    // 处理数据
    // 输出结果
    // printf("%d\\n", result);
    return 0;
}`,
    "cpp": `#include <iostream>
using namespace std;

int main() {
    // 从标准输入读取数据
    // int n;
    // cin >> n;
    // 处理数据
    // 输出结果
    // cout << result << endl;
    return 0;
}`,
    "java": `import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        // 从标准输入读取数据
        // int n = scanner.nextInt();
        // 处理数据
        // 输出结果
        // System.out.println(result);
        scanner.close();
    }
}`,
    "py2": `import sys

# 在这里编写你的代码
def solve():
    # 从标准输入读取数据
    # data = sys.stdin.readline().strip()
    # 处理数据
    # 输出结果
    # print result
    pass

if __name__ == "__main__":
    solve()`,
    "py3": `import sys

# 在这里编写你的代码
def solve():
    # 从标准输入读取数据
    # data = sys.stdin.readline().strip()
    # 处理数据
    # 输出结果
    # print(result)
    pass

if __name__ == "__main__":
    solve()`,
    "go": `package main

import (
    "fmt"
)

func main() {
    // 从标准输入读取数据
    // var n int
    // fmt.Scan(&n)
    // 处理数据
    // 输出结果
    // fmt.Println(result)
}`
};

// 加载代码模板
function loadCodeTemplate(language) {
    if (code_editor && codeTemplates[language]) {
        var template = codeTemplates[language];
        code_editor.setMarkdown(template);
    }
}

// 前端代码格式检查（与题目页保持同一套逻辑）
function checkCodeFormatFrontend(language, source) {
    if (!source || !source.trim()) {
        return { ok: true, message: null };
    }
    var lower = source.toLowerCase();
    var hasMainEntry = false;
    var hasIo = false;
    var hasFunctionLike = false;

    switch (language) {
        case "c":
        case "cpp":
            if (lower.indexOf(" main(") !== -1 || lower.indexOf(" main (") !== -1 ||
                lower.indexOf("int main(") !== -1 || lower.indexOf("void main(") !== -1) {
                hasMainEntry = true;
            }
            if (lower.indexOf("scanf(") !== -1 || lower.indexOf("printf(") !== -1 ||
                lower.indexOf(" cin") !== -1 || lower.indexOf("cout") !== -1 ||
                lower.indexOf("std::cin") !== -1 || lower.indexOf("std::cout") !== -1) {
                hasIo = true;
            }
            if (lower.indexOf("void ") !== -1 || lower.indexOf("int ") !== -1 ||
                lower.indexOf("double ") !== -1 || lower.indexOf("bool ") !== -1) {
                hasFunctionLike = true;
            }
            if (!hasMainEntry && !hasIo && hasFunctionLike) {
                return {
                    ok: false,
                    message: "检测到你的 C/C++ 代码可能只写了函数或模板，而没有 main() 和标准输入输出。本 OJ 不会自动调用你的函数，请提交完整可执行程序。"
                };
            }
            break;
        case "java":
            if (lower.indexOf("public static void main") !== -1) {
                hasMainEntry = true;
            }
            if (lower.indexOf("scanner ") !== -1 || lower.indexOf(" new scanner(") !== -1 ||
                lower.indexOf("system.in") !== -1 || lower.indexOf("system.out.print") !== -1) {
                hasIo = true;
            }
            if (lower.indexOf(" class ") !== -1 || lower.indexOf("interface ") !== -1) {
                hasFunctionLike = true;
            }
            if (!hasMainEntry && !hasIo && hasFunctionLike) {
                return {
                    ok: false,
                    message: "检测到你的 Java 代码可能只写了类/方法，而没有 public static void main(String[] args)。请提交包含 main 方法并处理输入输出的完整程序。"
                };
            }
            break;
        case "py2":
        case "py3":
            if (lower.indexOf("if __name__") !== -1 && lower.indexOf("__main__") !== -1) {
                hasMainEntry = true;
            }
            if (lower.indexOf("input(") !== -1 || lower.indexOf("raw_input(") !== -1 ||
                lower.indexOf("sys.stdin") !== -1) {
                hasIo = true;
            }
            if (lower.indexOf("def ") !== -1 || lower.indexOf("class ") !== -1) {
                hasFunctionLike = true;
            }
            if (!hasMainEntry && !hasIo && hasFunctionLike) {
                return {
                    ok: false,
                    message: "检测到你的 Python 代码可能只定义了函数/类，而没有主程序入口和标准输入输出。本 OJ 不会自动调用你定义的函数，请提交完整程序，例如包含 if __name__ == '__main__': 并使用 input()/sys.stdin 读取输入。"
                };
            }
            break;
        case "go":
            if (lower.indexOf("func main()") !== -1 || lower.indexOf("func main (") !== -1) {
                hasMainEntry = true;
            }
            if (lower.indexOf("fmt.scan") !== -1 || lower.indexOf("fmt.fscan") !== -1 ||
                lower.indexOf("fmt.println") !== -1 || lower.indexOf("fmt.printf") !== -1) {
                hasIo = true;
            }
            if (lower.indexOf("func ") !== -1) {
                hasFunctionLike = true;
            }
            if (!hasMainEntry && !hasIo && hasFunctionLike) {
                return {
                    ok: false,
                    message: "检测到你的 Go 代码可能只写了函数，而没有 func main() 和标准输入输出。请提交完整可执行程序。"
                };
            }
            break;
        default:
            return { ok: true, message: null };
    }

    return { ok: true, message: null };
}

function render_md() {
    $(function () {
        $(".md-text").each(function () {
            var tid = $(this).attr("id");
            $(this).attr("id", "editormd-view");
            editormd.markdownToHTML("editormd-view", {
                gfm: true,
                toc: true,
                tocm: false,
                tocStartLevel: 1,
                tocTitle: "目录",
                tocDropdown: false,
                tocContainer: "",
                markdown: "",
                autoLoadKaTeX: true,
                pageBreak: true,
                atLink: true,    // for @link
                emailLink: false,    // for mail address auto link
                tex: true,
                taskList: false,   // Github Flavored Markdown task lists
                flowChart: true,
                sequenceDiagram: true,
                previewCodeHighlight: true,
                htmlDecode: "style,script,iframe|on*",
            });
            $(this).attr("id", tid);
        });
    });
}

var cont = new Vue({
    el: "#contest-content",
    data: {
        cid: cid,
        password: "",
        attend: false,
        dataready: false,
        contest: {
            problems: []
        },
        pid: 1,
        problem: {
            problem: {
                timeLimit: "",
                memoryLimit: "",
                input: "",
                output: "",
                sampleInput: "",
                sampleOutput: "",
                hint: "",
            },
            tempTitle: ""
        },
        language: "c",
        share: false,
        timeLeft: "",
        percentage: 0,
        code: "",
        creator: false,
        completed: false
    },
    methods: {
        formatTime(ms) {
            if (!ms || ms === 0) return "0 ms";
            if (ms < 1000) return ms + " ms";
            return (ms / 1000).toFixed(2) + " s";
        },
        formatMemory(bytes) {
            if (!bytes || bytes === 0) return "0 B";
            if (bytes < 1024) return bytes + " B";
            if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + " KB";
            return (bytes / (1024 * 1024)).toFixed(2) + " MB";
        },
        init_window() {
            setInterval(this.time_left, 500);
            $(".progress").progress();
            var that = this;
            $(function () {
                code_editor = editormd("code-editor", {
                    width: "100%",
                    height: 500,
                    watch: false,
                    toolbar: false,
                    codeFold: true,
                    searchReplace: true,
                    autoFocus: false,
                    value: "",
                    placeholder: "Enjoy coding!",
                    editorTheme: "mdn-like",
                    previewTheme: "dark",
                    theam: "dark",
                    mode: "clike",
                    path: '/editor/lib/',
                });
                // 初始化时加载默认模板
                setTimeout(function() {
                    loadCodeTemplate(that.language);
                }, 100);
            });
        },
        time_left() {
            days_of_month = [0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];
            let dd = 0;
            let hh = 0;
            let mm = 0;
            let ss = 0;
            if (this.contest.started && !this.contest.ended) {
                dend = new Date(this.contest.endTime);
                dsta = new Date(this.contest.startTime);
                dn = new Date();
                d3 = new Date(dend - dn);
                ss = d3.getUTCSeconds();
                mm = d3.getUTCMinutes();
                hh = d3.getUTCHours();
                dd = d3.getUTCDate() - 1;
                dd += days_of_month[d3.getUTCMonth()];
                let len = dend - dsta;
                let gon = dn - dsta;
                per = gon / len * 100;
                $("#time-pogress").progress("set percent", per);
            }
            this.timeLeft = dd + (hh < 10 ? ":0" : ":") + hh + (mm < 10 ? ":0" : ":") + mm + (ss < 10 ? ":0" : ":") + ss;
            // setInterval(this.time_left,500);
        },
        change_lang() {
            if (this.language === "java") {
                code_editor.setCodeMirrorOption("mode", "clike");
            } else if (this.language.indexOf("py") === 0) {
                code_editor.setCodeMirrorOption("mode", "python");
            } else if (this.language.indexOf("c") === 0) {
                code_editor.setCodeMirrorOption("mode", "clike");
            } else if (this.language.indexOf("go") === 0) {
                code_editor.setCodeMirrorOption("mode", "go");
            }
            // 切换语言时加载对应的代码模板
            loadCodeTemplate(this.language);
        },
        change_problem(id) {
            if (id === this.pid) return;
            this.pid = id;
            this.dataready = false;
            for (var i = 0; i < this.contest.problems.length; i++) {
                if (this.contest.problems[i].tempId == this.pid) {
                    this.problem = this.contest.problems[i];
                    break;
                }
            }
            $("#problem-description").empty();
            $("#problem-description").append(" <textarea style=\"display: none;\"></textarea>");
            $("#problem-description").children("textarea").text(this.problem.problem.description);
            $("#problem-input").empty();
            $("#problem-input").append(" <textarea style=\"display: none;\"></textarea>");
            $("#problem-input").children("textarea").text(this.problem.problem.input);
            $("#problem-output").empty();
            $("#problem-output").append(" <textarea style=\"display: none;\"></textarea>");
            $("#problem-output").children("textarea").text(this.problem.problem.output);
            $("#problem-hint").empty();
            $("#problem-hint").append(" <textarea style=\"display: none;\"></textarea>");
            $("#problem-hint").children("textarea").text(this.problem.problem.hint);
            render_md();
            this.dataready = true;
        },
        submit() {
            this.code = code_editor.getMarkdown();
            if (this.code.length < 5) {
                alert("too short");
                return;
            }
            // 本地代码格式检查，避免明显的“只提交函数”导致误解
            var check = checkCodeFormatFrontend(this.language, this.code);
            if (!check.ok) {
                alert(check.message || "代码格式存在问题，请提交完整可执行程序（包含入口函数和标准输入输出）。");
                return;
            }
            var that = this;
            axios.post('/api/contest/submit/' + this.pid + "/" + cid, {
                language: that.language,
                source: that.code,
                share: that.share
            }, {
                headers: {
                    'Content-Type': 'application/json;charset=UTF-8'
                }
            }).then(function (res) {
                console.log(res.data);
                if (res.data && res.data.code != 200) {
                    var errorMsg = res.data.message || "提交失败，请稍后重试";
                    alert(errorMsg);
                } else {
                    scrollTo(0, 0);//x,y
                }
            }).catch(function (e) {
                var errorMsg = "提交失败";
                if (e.response && e.response.data) {
                    if (e.response.data.message) {
                        errorMsg = e.response.data.message;
                    } else if (e.response.data.code !== undefined) {
                        errorMsg = "错误代码: " + e.response.data.code;
                    }
                } else if (e.message) {
                    errorMsg = "网络错误: " + e.message;
                }
                alert(errorMsg);
                console.error("Submit error:", e);
            });
        },
        check_password() {
            var that = this;
            that.dataready = false;
            // this.attend=true;
            axios.get('/api/contest/' + cid + '?password=' + this.password)
                .then(function (response) {
                    that.contest = response.data;
                    if (that.contest.password != "password") {
                        that.attend = true;
                        that.problem = that.contest.problems[0]
                        that.pid = that.problem.tempId;
                        render_md();
                        that.init_window();
                        that.dataready = true;
                        that.checkCompletionStatus();
                    } else {
                        that.attend = false;
                    }
                });
        },
        checkCompletionStatus() {
            var that = this;
            axios.get('/api/contest/complete/' + cid)
                .then(function (response) {
                    if (response.data && response.data.code === 200) {
                        that.completed = response.data.message === "completed";
                    }
                })
                .catch(function (e) {
                    console.log("Error checking completion status:", e);
                });
        },
        completeContest() {
            var that = this;
            if (!confirm("确定要完成比赛吗？此操作不可逆，您将无法继续提交代码。")) {
                return;
            }
            axios.post('/api/contest/complete/' + cid)
                .then(function (response) {
                    if (response.data && response.data.code === 200) {
                        that.completed = true;
                        alert("比赛已完成！");
                    } else {
                        var errorMsg = response.data.message || "完成比赛失败";
                        alert(errorMsg);
                    }
                })
                .catch(function (e) {
                    var errorMsg = "完成比赛失败";
                    if (e.response && e.response.data) {
                        if (e.response.data.message) {
                            errorMsg = e.response.data.message;
                        }
                    }
                    alert(errorMsg);
                    console.error("Error completing contest:", e);
                });
        }
    },
    created() {
        var that = this;
        that.dataready = false;
        axios.get('/api/contest/' + cid)
            .then(function (response) {
                that.contest = response.data;
                if (that.contest.password != "password") {
                    that.attend = true;
                    if (that.contest.problems.length > 0) {
                        that.problem = that.contest.problems[0];
                        that.pid = that.problem.tempId;
                    }
                    render_md();
                    $('title').text(that.contest.title);
                    that.init_window();
                    that.dataready = true;
                    axios.get("/api/contest/background/access/" + cid)
                        .then(function (res) {
                            if (res.data == "success") {
                                that.creator = true;
                            }
                        }).catch(function (e) {
                        console.log(e)
                    });
                    that.checkCompletionStatus();
                } else {
                    that.attend = false;
                }
            }).catch(function (e) {
            if (e.response.status == 404) {
                location.href = "/contest";
            }
            console.log(e);
        });
    }
});