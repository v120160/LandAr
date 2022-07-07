package hu.hero.landar.net;

import android.content.Context;
import android.util.Log;


import java.util.List;

import hu.hero.landar.Geo.Point3;
import hu.hero.landar.MainActivity;
import hu.hero.landar.database.PICDATA;
import hu.hero.landar.database.PICDATA3D;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;


public class GetDataByDistance{
    private String  mUserId;
    private Context mContext;
    private String  mResponse;

    public class SpatialIndexPackage{
        public List<Integer> parnums;
        public List<List<Point3>> ptlists;
        public List<PICDATA3D> pics;
    }

    // 內崁 service
    private interface MyAPIService {
        @GET("/distanceSearch")
        Call<SpatialIndexPackage> distanceSearch(@Query("userId") String user,
                                             @Query("lat") double lat,
                                             @Query("lon") double lon,
                                             @Query("distance") double distance );
    }

    public GetDataByDistance(Context context) {
        mContext = context;
    }

    public void get(String userid , double lat , double lon , double distance ) {
        mUserId = userid;
//        String url = LitePal.findFirst( SETTING.class ).getServerUrl();
        String url = "http://192.168.1.25:8082";
//        String url = "http://192.168.0.106:8082";

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        MyAPIService service = retrofit.create(MyAPIService.class);

        Call<SpatialIndexPackage> call = service.distanceSearch(userid, lat, lon, distance );

        // 此段為非同步請求
        call.enqueue(new Callback<SpatialIndexPackage>() {
            public void onResponse(Call<SpatialIndexPackage> call, Response<SpatialIndexPackage> response) {
                // 連線成功
                SpatialIndexPackage pack = response.body();
//                Log.d("title", list.toString());
                ((MainActivity)mContext).onReadDataFinish(pack);
            }

            @Override
            public void onFailure(Call<SpatialIndexPackage> call, Throwable t) {
                // 連線失敗
                Log.d("title", "Fail");
                ((MainActivity)mContext).onReadDataFinish(null);
            }
        });

    }
}
