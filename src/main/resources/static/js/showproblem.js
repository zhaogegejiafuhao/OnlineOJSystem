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

// 前端代码格式检查（与后端逻辑保持一致方向，略简化）
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

$(function () {
    code_editor = editormd("code-editor", {
        width: "100%",
        gfm: true,
        height: 300,
        watch: false,
        toolbar: false,
        codeFold: true,
        searchReplace: true,
        autoFocus: false,
        placeholder: "Enjoy coding!",
        editorTheme: "mdn-like",
        previewTheme: "dark",
        theam: "dark",
        mode: "clike",
        path: '/editor/lib/',
        pluginPath: "/editor/plugins/"
    });
    // 初始化时加载默认模板（C语言）
    setTimeout(function() {
        loadCodeTemplate("c");
    }, 100);
});
var prom = new Vue({
    el: '#vue-problem',
    data: {
        problem: {},
        code: "",
        language: "c",
        share: true,
        dataready: false,
        isAccepted: false,
        ready: false,
        tags: [],
        color: ["red", "blue", "green", "orange", "yellow", "pink", "brown", "purple", "olive", "teal"],
        status: [],
        problem_id: pid
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
        get_history_data() {
            var that = this;
            axios.get('/api/status/user/latest/submit/' + pid)
                .then(function (res) {
                that.status = res.data.data;
            });
        },
        colorClass() {
            return this.color[Math.floor(Math.random() * 10)]
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
        submit: function () {
            this.code = code_editor.getMarkdown();
            // 本地代码格式检查，避免明显的“只提交函数”导致误解
            var check = checkCodeFormatFrontend(this.language, this.code);
            if (!check.ok) {
                alert(check.message || "代码格式存在问题，请提交完整可执行程序（包含入口函数和标准输入输出）。");
                return;
            }
            var that = this;
            axios.post('/api/problems/submit/' + pid, {
                language: that.language,
                source: that.code,
                share: that.share
            }, {
                headers: {
                    'Content-Type': 'application/json;charset=UTF-8'
                }
            }).then(function (res) {
                if (res.data && res.data.code != 200) {
                    var errorMsg = res.data.message || "提交失败，请稍后重试";
                    alert(errorMsg);
                    return;
                } else {
                    that.get_history_data();
                    scrollTo(0, 0);//x,y
                }
            }).catch(function (error) {
                var errorMsg = "提交失败";
                if (error.response && error.response.data) {
                    if (error.response.data.message) {
                        errorMsg = error.response.data.message;
                    } else if (error.response.data.code) {
                        errorMsg = "错误代码: " + error.response.data.code;
                    }
                } else if (error.message) {
                    errorMsg = "网络错误: " + error.message;
                }
                alert(errorMsg);
                console.error("Submit error:", error);
            });
        }
    },
    created() {
        var that = this;
        axios.get('/api/problems/' + pid).then(function (response) {
            if (response.data.code != 200) {
                return;
            }
            that.problem = response.data.data;
            that.tags = that.problem.tags;
            that.ready = true;
            $("title").text(response.data.data.title);
            $(function () {
                $(".md-text").each(function () {
                    $(this).attr("id", "editormd-view");
                    editormd.markdownToHTML("editormd-view", {
                        gfm: true,
                        htmlDecode: "style,script,iframe|on*",
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
                    });
                    $(this).attr("id", "");
                });
                if (typeof renderMathInElement === 'function') {
                    renderMathInElement(document.body, {
                        delimiters: [
                            {left: "$$", right: "$$", display: true},
                            {left: "$", right: "$", display: false},
                            {left: "\\(", right: "\\)", display: false},
                            {left: "\\[", right: "\\]", display: true}
                        ],
                        ignoredTags: ["script", "noscript", "style", "textarea", "pre", "code", "option"]
                    });
                }
            });
            that.dataready = true;
        }).catch(function (e) {
            console.log(e);
            location.href = "/4O4";
        });
        axios.get("/api/problems/is/accepted/" + pid)
            .then(function (res) {
                if (res.data.message == 'success') {
                    that.isAccepted = res.data.data;
                } else {
                    console.log(res.data);
                }
            }).catch(function (e) {
            console.log(e);
        });
        this.get_history_data();
    }
});