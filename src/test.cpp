#include <iostream>
#include <string>
#include <algorithm>
#include <cstring>
#include <queue>
using namespace std;

typedef long long LL;
typedef pair<int, int> PII;
const int N = 100010;
int n, last;
int q[N];
char str[1000];

PII get()
{
    int len = strlen(str);
    int day = 0, i = 0;
    for(; i < len && str[i] != ' '; ++i)
    {
        day = day * 10 + str[i] - '0';
    }
    i ++;
    int res = 0;
    for(; i < len; ++i)
    {
        res = res * 10 + str[i] - '0';
    }
    return {day, res};
}

int main()
{
    cin >> n;
    int hh = 0, tt = -1;
    double ans = 0;

    int start, end;
    getchar();
    gets(str);
    auto t = get();
    int d = t.first, x = t.second;
    last = start = end = d;
    q[++ tt] = x;
    int cnt = 0, poped = 0;
    for(int i = 1; i < 2 * n; ++i)
    {
        gets(str);
        auto t = get();
        int d = t.first, x = t.second;
        end += d;
        if(!x) cnt ++;
        if(hh > tt)
        {
            if(x) q[++ tt] = x;
        }
        else
        {
            if(x)
            {
                int head = q[hh];
                while(hh <= tt)
                {
                    if(q[tt] < x) 
                    {
                        poped ++;
                        tt --;
                    }
                    else break;
                }
                q[++ tt] = x;
                ans += (LL) head * d;
            }
            else
            {
                int head = q[hh];
                if(poped < cnt)
                {
                    poped ++;
                    hh ++;
                }
                ans += (LL) d * head;
            }
        }
    }
    cout << end << endl;
    printf("%.2f\n", ans );
    return 0;
}

/*
6
2 100
2 200
5
1 150
1
2 100
2 300
2 200
2
3
1
2



6
0 100
1 200
2 300
0
2
2 100
0
1 400
1
2 100
0
4
*/
