package com.example.mymap;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.InfoWindow;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.overlayutil.BikingRouteOverlay;
import com.baidu.mapapi.overlayutil.DrivingRouteOverlay;
import com.baidu.mapapi.overlayutil.IndoorRouteOverlay;
import com.baidu.mapapi.overlayutil.MassTransitRouteOverlay;
import com.baidu.mapapi.overlayutil.TransitRouteOverlay;
import com.baidu.mapapi.overlayutil.WalkingRouteOverlay;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.route.BikingRouteResult;
import com.baidu.mapapi.search.route.DrivingRouteResult;
import com.baidu.mapapi.search.route.IndoorRouteResult;
import com.baidu.mapapi.search.route.MassTransitRouteResult;
import com.baidu.mapapi.search.route.OnGetRoutePlanResultListener;
import com.baidu.mapapi.search.route.PlanNode;
import com.baidu.mapapi.search.route.RoutePlanSearch;
import com.baidu.mapapi.search.route.TransitRouteResult;
import com.baidu.mapapi.search.route.WalkingRoutePlanOption;
import com.baidu.mapapi.search.route.WalkingRouteResult;

public class MainActivity extends Activity {
    private MapView mMapView = null;
    private BaiduMap mBaiduMap = null;
    private LocationClient mLocationClient = null;
    private BaiduMap.OnMarkerClickListener onMarkerClickListener = null;
    private RoutePlanSearch mSearch = null;
    private LatLng mLocation = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //获取地图控件引用
        mMapView = (MapView) findViewById(R.id.bmapView);
        mBaiduMap = mMapView.getMap();
        mBaiduMap.setMyLocationEnabled(true);
        mSearch = RoutePlanSearch.newInstance();
        mSearch.setOnGetRoutePlanResultListener(routePlanResultListener);

        // 设置地图中心点为辽宁沈阳东北大学浑南校区五舍
        mBaiduMap.setMapStatus(MapStatusUpdateFactory.newLatLng(new LatLng(41.6577396168, 123.4343104372)));
        // 调整缩放级别
        mBaiduMap.setMapStatus(MapStatusUpdateFactory.zoomTo(18));
        Toast.makeText(getApplicationContext(), "正在定位", Toast.LENGTH_SHORT).show();

        // 开启定位
        initLocation();
        // 设置单击事件
        initClick();
    }

    private void initLocation() {
        //定位初始化
        mLocationClient = new LocationClient(this);

        //通过LocationClientOption设置LocationClient相关参数
        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(true); // 打开gps
        option.setCoorType("bd09ll"); // 设置坐标类型
        option.setScanSpan(1000);

        //设置locationClientOption
        mLocationClient.setLocOption(option);

        //注册LocationListener监听器
        MyLocationListener myLocationListener = new MyLocationListener();
        mLocationClient.registerLocationListener(myLocationListener);
        //开启地图定位图层
        mLocationClient.start();
    }

    private void initClick() {
        BaiduMap.OnMapClickListener onMapClickListener = new BaiduMap.OnMapClickListener() {
            /**
             * 地图单击事件回调函数
             *
             * @param point 点击的地理坐标
             */
            @Override
            public void onMapClick(LatLng point) {
            }

            /**
             * 地图内 Poi 单击事件回调函数
             *
             * @param mapPoi 点击的 poi 信息
             */
            @Override
            public boolean onMapPoiClick(MapPoi mapPoi) {
                showSearchButton(mapPoi.getPosition());
                return true;
            }
        };
        //设置地图单击事件监听
        mBaiduMap.setOnMapClickListener(onMapClickListener);

        onMarkerClickListener = new BaiduMap.OnMarkerClickListener() {
            /**
             * 地图 Marker 覆盖物点击事件监听函数
             * @param marker 被点击的 marker
             */
            @Override
            public boolean onMarkerClick(Marker marker) {
                return false;//是否捕获点击事件
            }
        };

        // 设置地图 Marker 覆盖物点击事件监听者,自3.4.0版本起可设置多个监听对象，停止监听时调用removeMarkerClickListener移除监听对象
        mBaiduMap.setOnMarkerClickListener(onMarkerClickListener);
    }

    private void showSearchButton(final LatLng location) {
        //用来构造InfoWindow的Button
        Button button = new Button(getApplicationContext());
        button.setText("到这去");

        //构造InfoWindow
        //-10 InfoWindow相对于point在y轴的偏移量
        InfoWindow mInfoWindow = new InfoWindow(button, location, -10);

        //使InfoWindow生效
        mBaiduMap.showInfoWindow(mInfoWindow);

        // 按钮点击监听
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBaiduMap.clear();

                PlanNode stNode;
                if (mLocation != null) stNode = PlanNode.withLocation(mLocation);
                else stNode = PlanNode.withLocation(new LatLng(41.6577396168, 123.4343104372));
                PlanNode enNode = PlanNode.withLocation(location);
                mSearch.walkingSearch((new WalkingRoutePlanOption())
                        .from(stNode)
                        .to(enNode));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView. onResume ()，实现地图生命周期管理
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView. onPause ()，实现地图生命周期管理
        mMapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
        mLocationClient.stop();
        mSearch.destroy();
        mBaiduMap.setMyLocationEnabled(false);
        //停止监听时移除监听对象
        mBaiduMap.removeMarkerClickListener(onMarkerClickListener);
        mMapView.onDestroy();
        mMapView = null;
    }

    // 定位监听器
    public class MyLocationListener extends BDAbstractLocationListener {
        @Override
        public void onReceiveLocation(BDLocation location) {
            //mapView 销毁后不在处理新接收的位置
            if (location == null || mMapView == null) {
                return;
            }
            double mLatitude = location.getLatitude();
            double mLongitude = location.getLongitude();

            if (mLatitude != Double.MIN_VALUE && mLongitude != Double.MIN_VALUE) {
                MyLocationData locData = new MyLocationData.Builder()
                        .accuracy(location.getRadius())
                        // 此处设置开发者获取到的方向信息，顺时针0-360
                        .direction(location.getDirection()).latitude(mLatitude)
                        .longitude(mLongitude).build();
                mBaiduMap.setMyLocationData(locData);
                mLocation = new LatLng(mLatitude,mLongitude);
            }
        }
    }

    // 路线规划监听
    OnGetRoutePlanResultListener routePlanResultListener = new OnGetRoutePlanResultListener() {
        @Override
        public void onGetWalkingRouteResult(WalkingRouteResult walkingRouteResult) {
            if (walkingRouteResult == null) {
                return;
            } else if (walkingRouteResult.error != SearchResult.ERRORNO.NO_ERROR) {
                Toast.makeText(getApplicationContext(), "路线查找失败 " + walkingRouteResult.error,
                        Toast.LENGTH_SHORT).show();
                return;
            }

            //创建WalkingRouteOverlay实例
            WalkingRouteOverlay overlay = new WalkingRouteOverlay(mBaiduMap);
            if (walkingRouteResult.getRouteLines().size() > 0) {
                //获取路径规划数据,(以返回的第一条数据为例)
                //为WalkingRouteOverlay实例设置路径数据
                overlay.setData(walkingRouteResult.getRouteLines().get(0));
                //在地图上绘制WalkingRouteOverlay
                overlay.addToMap();
                //Toast.makeText(MainActivity.this, "知道了2", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onGetBikingRouteResult(BikingRouteResult bikingRouteResult) {
            //创建BikingRouteOverlay实例
            BikingRouteOverlay overlay = new BikingRouteOverlay(mBaiduMap);
            if (bikingRouteResult.getRouteLines().size() > 0) {
                //获取路径规划数据,(以返回的第一条路线为例）
                //为BikingRouteOverlay实例设置数据
                overlay.setData(bikingRouteResult.getRouteLines().get(0));
                //在地图上绘制BikingRouteOverlay
                overlay.addToMap();
            }
        }

        @Override
        public void onGetIndoorRouteResult(IndoorRouteResult indoorRouteResult) {
            //创建IndoorRouteOverlay实例
            IndoorRouteOverlay overlay = new IndoorRouteOverlay(mBaiduMap);
            if (indoorRouteResult.getRouteLines() != null && indoorRouteResult.getRouteLines().size() > 0) {
                //获取室内路径规划数据（以返回的第一条路线为例）
                //为IndoorRouteOverlay实例设置数据
                overlay.setData(indoorRouteResult.getRouteLines().get(0));
                //在地图上绘制IndoorRouteOverlay
                overlay.addToMap();
            }
        }

        @Override
        public void onGetDrivingRouteResult(DrivingRouteResult drivingRouteResult) {
            //创建DrivingRouteOverlay实例
            DrivingRouteOverlay overlay = new DrivingRouteOverlay(mBaiduMap);
            if (drivingRouteResult.getRouteLines().size() > 0) {
                //获取路径规划数据,(以返回的第一条路线为例）
                //为DrivingRouteOverlay实例设置数据
                overlay.setData(drivingRouteResult.getRouteLines().get(0));
                //在地图上绘制DrivingRouteOverlay
                overlay.addToMap();
            }
        }

        @Override
        public void onGetMassTransitRouteResult(MassTransitRouteResult massTransitRouteResult) {
            //创建MassTransitRouteOverlay实例
            MassTransitRouteOverlay overlay = new MassTransitRouteOverlay(mBaiduMap);
            if (massTransitRouteResult.getRouteLines() != null && massTransitRouteResult.getRouteLines().size() > 0) {
                //获取路线规划数据（以返回的第一条数据为例）
                //为MassTransitRouteOverlay设置数据
                overlay.setData(massTransitRouteResult.getRouteLines().get(0));
                //在地图上绘制Overlay
                overlay.addToMap();
            }
        }

        @Override
        public void onGetTransitRouteResult(TransitRouteResult transitRouteResult) {
            //创建TransitRouteOverlay实例
            TransitRouteOverlay overlay = new TransitRouteOverlay(mBaiduMap);
            //获取路径规划数据,(以返回的第一条数据为例)
            //为TransitRouteOverlay实例设置路径数据
            if (transitRouteResult.getRouteLines().size() > 0) {
                overlay.setData(transitRouteResult.getRouteLines().get(0));
                //在地图上绘制TransitRouteOverlay
                overlay.addToMap();
            }
        }
    };
}