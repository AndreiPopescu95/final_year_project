//
// Created by andrei on 04/03/18.
//

using namespace std;



void Commands::move(double distance, String direction) {
    geometry_msgs::Twist vel_msg;
    bool isForward = false;

    if(direction == "forward"){
        isForward = true;
    }

    if(isForward)
        vel_msg.linear.x = 1;
    else
        vel_msg.linear.x= -1;


    double t0 =ros::Time::now().toSec();
    cout<<"time0: "<< t0<<endl;
    double current_distance =0;
    ros::Rate loop_rate(1000);

    int ok=0;
    float temp=0;
    do{
        velocity_publisher.publish(vel_msg);
        double t1=ros::Time::now().toSec();
        current_distance = speed * ((t1-t0)-(temp*2));

        if(ok==0 && current_distance > 0.5) {
            temp = current_distance-0.5;
            current_distance = 0.5;
            t1=1;
            ok++;

        }
        /*cout<<"Current_distance: "<< current_distance<<endl;
        cout<<"time: "<< t1-t0<<endl;
        cout<<"time1: "<< t1<<endl;*/

        ros::spinOnce();
        loop_rate.sleep();


    }while(current_distance <distance);
    vel_msg.linear.x=0;
    velocity_publisher.publish(vel_msg);

}

void Commands::move(double distance, String direction, double angle) {
    geometry_msgs::Twist vel_msg;
    vel_msg.linear.x=0;
    vel_msg.linear.y=0;
    vel_msg.linear.z=0;

    vel_msg.angular.x=0;
    vel_msg.angular.y=0;

    if(clock_wise)
        vel_msg.angular.z=-1;
    else
        vel_msg.angular.z=1;

    double current_angle =0.0;
    double t0 = ros::Time::now().toSec();
    ros::Rate loop_rate(1000);
    int ok=0;
    float temp=0;
    cout<<"desider angle: "<<angle<<endl;
    do{
        velocity_publisher.publish(vel_msg);

        double t1= ros::Time::now().toSec();


        ok++;
        if(ok==2 ) {

            //    temp = t1-1;


        }
        //  t1=t1-temp;
        current_angle= 1 * ((t1-t0)-temp);

        cout<<"Current_angle: "<< current_angle<<endl;

        cout<<"t1: "<< t1<<endl;

        ros::spinOnce();
        loop_rate.sleep();

    }while(current_angle<angle);
    vel_msg.angular.z=0;
    velocity_publisher.publish(vel_msg);
}