var cid = location.pathname.split('/');
cid = cid[cid.length - 1];
vue_solu = new Vue({
    el: "#vue-solutions",
    data: {
        contest: {
            id: cid
        },
        solutions: [],
        totalElements: 0,
        totalPages: 0,
        last: true,
        number: 0,
        size: 30,
        numberOfElements: 0,
        first: true,
        empty: true,
        npage: [],
        isAdmin: false,
        viewMode: 'mine' // 'mine' or 'all'
    },
    created: function () {
        var that = this;
        // Check if user is admin
        axios.get('/api/user/permission').then(function (res) {
            if (res.data && res.data.data) {
                var permission = res.data.data;
                if (permission === 'admin' || permission === 'teacher') {
                    that.isAdmin = true;
                }
            }
            that.loadData();
        }).catch(function (e) {
            console.log("Error checking permission:", e);
            that.loadData();
        });
    },
    methods: {
        formatMemory(bytes) {
            if (!bytes || bytes === 0) {
                return "0 B";
            }
            if (bytes < 1024) {
                return bytes + " B";
            } else if (bytes < 1024 * 1024) {
                return (bytes / 1024).toFixed(2) + " KB";
            } else {
                return (bytes / (1024 * 1024)).toFixed(2) + " MB";
            }
        },
        loadData(page) {
            if (page === undefined) page = 0;
            var that = this;
            var url = '/api/contest/status/' + cid;
            if (this.viewMode === 'all' && this.isAdmin) {
                url = '/api/contest/status/' + cid + '/all';
            }
            url += '?page=' + page;
            axios.get(url).then(function (res) {
                console.log("Response received:", res.data);
                if (res.data && res.data.content && Array.isArray(res.data.content)) {
                    that.solutions = res.data.content;
                    that.totalPages = res.data.totalPages || 0;
                    that.totalElements = res.data.totalElements || 0;
                    that.last = res.data.last !== undefined ? res.data.last : true;
                    that.number = res.data.number || 0;
                    that.size = res.data.size || 30;
                    that.first = res.data.first !== undefined ? res.data.first : true;
                    that.numberOfElements = res.data.numberOfElements || 0;
                    that.empty = res.data.empty !== undefined ? res.data.empty : (res.data.content.length === 0);
                    that.npage = [];
                    for (var i = Math.max(0, that.number - 3); i < Math.min(that.totalPages, that.number + 3); i++) {
                        that.npage.push(i)
                    }
                    console.log("Loaded " + that.solutions.length + " solutions, page " + that.number + " of " + that.totalPages);
                } else {
                    console.error("Unexpected response format:", res.data);
                    console.error("Response structure:", {
                        hasData: !!res.data,
                        hasContent: !!(res.data && res.data.content),
                        isArray: !!(res.data && res.data.content && Array.isArray(res.data.content)),
                        data: res.data
                    });
                    that.solutions = [];
                    that.empty = true;
                }
            }).catch(function (e) {
                console.error("Error loading solutions:", e);
                console.error("Error details:", {
                    status: e.response ? e.response.status : 'N/A',
                    statusText: e.response ? e.response.statusText : 'N/A',
                    data: e.response ? e.response.data : 'N/A',
                    message: e.message
                });
                that.solutions = [];
                that.empty = true;
                var errorMsg = "加载数据失败";
                if (e.response) {
                    if (e.response.status === 404) {
                        errorMsg = "比赛不存在或未开始";
                    } else if (e.response.status === 403) {
                        errorMsg = "权限不足，请先登录";
                    } else if (e.response.status === 500) {
                        errorMsg = "服务器错误，请稍后重试";
                        if (e.response.data && e.response.data.message) {
                            errorMsg += "：" + e.response.data.message;
                        }
                    } else if (e.response.data && e.response.data.message) {
                        errorMsg = e.response.data.message;
                    } else {
                        errorMsg = "请求失败（状态码：" + e.response.status + "）";
                    }
                } else if (e.message) {
                    errorMsg = "网络错误：" + e.message;
                }
                alert(errorMsg);
            });
        },
        get_page(page) {
            if (page >= this.totalPages || page < 0) return;
            this.loadData(page);
        },
        toggleViewMode() {
            if (this.viewMode === 'mine') {
                this.viewMode = 'all';
            } else {
                this.viewMode = 'mine';
            }
            this.loadData(0);
        }
    }
})