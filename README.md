框架简介<br>
=
PullRefeshLayout是一个支持下拉刷新，上拉加载的ViewGroup类型组件。
它的特点是：使用简单，可自定义刷新动画效果，支持所有基础控件
（例如RecyclerView、TextView、ListView、ScrollView、WebView、
LinearLayout等等 ），功能强大但是库文件很小。 默认为箭头风格的刷新
和加载动画，完全可以自定义任何风格的刷新动画效果（自定义帧动画，或者
其他任何效果）。（实现任何你想要的风格的效果，主要包括：自动触发加载更多
、自动触发刷新、设置刷新控件的位置：前覆盖式，后覆盖，非覆盖）

<img src="https://github.com/wbz360/PullRefreshLayout/raw/master/screenshot/0.jpg" height="460px" width="280px" /><img src="https://github.com/wbz360/PullRefreshLayout/raw/master/screenshot/1.jpg" height="460px" width="280px" /><img src="https://github.com/wbz360/PullRefreshLayout/raw/master/screenshot/2.jpg" height="460px" width="280px" /><img src="https://github.com/wbz360/PullRefreshLayout/raw/master/screenshot/3.jpg" height="460px" width="280px" /><img src="https://github.com/wbz360/PullRefreshLayout/raw/master/screenshot/4.jpg" height="460px" width="280px" /><img src="https://github.com/wbz360/PullRefreshLayout/raw/master/screenshot/5.jpg" height="460px" width="280px" /><img src="https://github.com/wbz360/PullRefreshLayout/raw/master/screenshot/6.jpg" height="460px" width="280px" />
使用方法
==

（1）编写XML文件
--
>将需要有刷新或者加载功能的View用PullRefeshLayout包含，作为PullRefeshLayout的子view，PullRefeshLayout的XML属性全部继承自FrameLayout，你就把它当做FrameLayout使用就行了，唯一要注意的是：PullRefeshLayout包含的子一级子View只能有一个。这里以ListView为例，其他View类似。

```java 
    <com.appdsn.pullrefreshlayout.PullRefreshLayout 
        xmlns:android="http://schemas.android.com/apk/res/android"
        android:id="@+id/refreshLayout"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:background="#ffffff"
        android:orientation="vertical" >
    
        <ListView
            android:id="@+id/listView"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:divider="#9a9a9a"
            android:background="#20000000"
            android:dividerHeight="1px"
            android:scrollbars="none"/>
    
    </com.appdsn.pullrefreshlayout.PullRefreshLayout>
 ```
 
（2）代码中监听刷新和加载
-

```java   
            //PullRefreshLayout的监听设置
            refreshLayout.setOnRefreshListener(new OnRefreshListener() {
                @Override
                public void onRefresh(PullRefreshLayout pullRefreshLayout) {
                    //模仿从网上获取数据
                    pullRefreshLayout.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            refreshLayout.finishRefresh();//刷新完成后隐藏刷新View
                        }
                    }, 3000);
                }
    
                @Override
                public void onLoadMore(PullRefreshLayout pullRefreshLayout) {
                    pullRefreshLayout.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            //加载完成后隐藏加载的View，其中true告诉PullRefreshLayout还有数据，可以进行下一次上拉加载
                            refreshLayout.finishLoadMore(true);
                        }
                    }, 3000);
                }
            });
 ```

（3）其他设置项
-

```java
{
    refreshLayout.autoRefresh(); //自动刷新   
    refreshLayout.setRefreshEnable(false);//禁用下拉刷新
    refreshLayout.setLoadMoreEnable(false);//禁用上拉加载
    refreshLayout.setAutoLoadEnable(false);//禁用上拉自动加载
    refreshLayout.setHeaderPosition(PullRefreshLayout.POSITION_TOP);//设置刷新View的位置：覆盖在内容View的前面，覆盖在后面，相接在上面
    refreshLayout.setFooterPosition(PullRefreshLayout.POSITION_TOP);
    refreshLayout.setOffsetRadio(2.0f);//拉动阻力系数
    refreshLayout.setStartRefreshDistance(100);//拉动到某个位置松手后刷新，默认是headerView高度
    refreshLayout.setStartLoadMoreDistance(100);//拉动到某个位置松手后加载，默认是footerView高度
 }
 ```

（4）自定义刷新View动画
-
>默认是箭头风格的刷新加载动画，如果不满足需求可以自定义动画，非常简单，只需要继承IRefreshView接口，重新其中的方法即可，下面以自定义帧动画为例：
```java
public class CustomHeaderView implements IRefreshView {
    private AnimationDrawable animDrawable;
    private ImageView imgAnim;
    private View headerView;
    public CustomHeaderView(Context context) {
        headerView = LayoutInflater.from(context).inflate(R.layout.layout_header_view, null);
        imgAnim = (ImageView) headerView.findViewById(R.id.ivAnim);
        animDrawable = (AnimationDrawable) ivAnim.getDrawable();

        /*设置HeaderView尺寸*/
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp2px(160));
        params.gravity = Gravity.CENTER;
        headerView.setLayoutParams(params);
    }

    //添加到头部的View
    @Override
    public View getRefreshView() {
        return headerView;
    }
    //小于等于0，默认为HeaderView本身的高度
    @Override
    public int getStartRefreshDistance() {
        return 0;
    }
    
    //拉动开始时回调：可以做一些初始化操作
    @Override
    public void onStart(PullRefreshLayout pullRefreshLayout) {
        animDrawable.setVisible(true, true);//重置起始帧
        imgAnim.setPivotX(ivAnim.getWidth() / 2);
        imgAnim.setPivotY(ivAnim.getHeight());
        imgAnim.setTranslationX(ivAnim, 0);
    }

    //刷新完成后：可以显示刷新完成的标志，比如暂时=停动画
    @Override
    public void onComplete(PullRefreshLayout pullRefreshLayout, boolean hasMoreData) {
        animDrawable.stop();
    }

    //拉动过程中：可以根据拉动的比例percent自定义一些效果，比如放大View
    @Override
    public void onPull(PullRefreshLayout pullRefreshLayout, float percent) {
        imgAnim.setScaleX(percent);
        imgAnim.setScaleY(percent);
    }

    //正在刷新的回调：比如开始一个动画
    @Override
    public void onRefresh(PullRefreshLayout pullRefreshLayout) {
        animDrawable.start();
    }

}
 ```

联系方式
-
* Email：2792889279@qq.com
* qq： 2792889279

Licenses
-
        
        Copyright 2018 wbz360(王宝忠)

        Licensed under the Apache License, Version 2.0 (the "License");
        you may not use this file except in compliance with the License.
        You may obtain a copy of the License at

         　　　　http://www.apache.org/licenses/LICENSE-2.0

        Unless required by applicable law or agreed to in writing, software
        distributed under the License is distributed on an "AS IS" BASIS,
        WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
        See the License for the specific language governing permissions and
        limitations under the License.






