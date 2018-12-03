//
// Created by andrei on 03/03/18.
//

#include "ros/ros.h"
#include "geometry_msgs/Twist.h"
ros::Publisher velocity_publisher;
ros:: Subscriber pose_subscriber;

const double PI =3.14159265359;

using namespace std;


void move(double speed, double distance, bool isForward);
void rotate(double angular_speed, double angle, bool clock_wise);
double degrees2radians(double angle_in_degrees);
void testRun(double forward_speed,double angular_speed,double angle);
void wait_();

int main(int argc, char **argv)
{

    ros::init(argc, argv, "commands");
    ros::NodeHandle n;
    velocity_publisher = n.advertise<geometry_msgs::Twist>("cmd_vel",10);

    //initatiate variables
    double forward_speed=30,angular_speed=30,angle=90,distance;
    bool isForward, clockwise=true;

    //move forward
    // move (degrees2radians(forward_speed),10.0,1);
    // rotate
    // rotate( degrees2radians(angular_speed), degrees2radians(angle), clockwise);

    //Run test
    wait_();
    testRun(forward_speed,angular_speed,angle);

}

void wait_(){
    int count =1;
    int max_count =10;
    geometry_msgs::Twist msg;

    msg.linear.x = 0.0;
    msg.angular.z = 0.0;

    ros::Rate rate(10);

    while(count<=max_count){
        cout<<"wait_ count: "<<count<<endl;
        velocity_publisher.publish(msg);
        count+=1;
        rate.sleep();
    }
}

void testRun(double forward_speed,double angular_speed,double angle){
    //Move 1 metre - Turn 90 clockwise - move 1 metre - Turn 90 anti-clockwise - move 1 metre - Turn 90 anti-clockwise - move 1 metre
    //Turn 90 clockwise - Move 1 metre -
    //Turn 90 anti-clockwise - move 1 metre - Turn 90 clockwise - move 1 metre - Turn 90 clockwise - move 1 metre
    //Turn 90 anti-clockwise - Move 1 metre
    bool clockwise=true;
    bool anticlockwise=false;
    wait_();

    //move forward
    move (degrees2radians(forward_speed),1.0,1);
    wait_();
    // rotate
    rotate( degrees2radians(angular_speed), degrees2radians(angle), clockwise);
    wait_();
    //move forward
    move (degrees2radians(forward_speed),1.0,1);
    wait_();
    // rotate
    rotate( degrees2radians(angular_speed), degrees2radians(angle), anticlockwise);
    wait_();
    //move forward
    move (degrees2radians(forward_speed),1.0,1);
    wait_();
    // rotate
    rotate( degrees2radians(angular_speed), degrees2radians(angle), anticlockwise);
    wait_();
    //move forward
    move (degrees2radians(forward_speed),1.0,1);
    wait_();
    // rotate
    rotate( degrees2radians(angular_speed), degrees2radians(angle), clockwise);
    wait_();
    //move forward
    move (degrees2radians(forward_speed),1.0,1);
    wait_();
    // rotate
    rotate( degrees2radians(angular_speed), degrees2radians(angle), anticlockwise);
    wait_();
    //move forward
    move (degrees2radians(forward_speed),1.0,1);
    wait_();
    // rotate
    rotate( degrees2radians(angular_speed), degrees2radians(angle), clockwise);
    wait_();
    //move forward
    move (degrees2radians(forward_speed),1.0,1);
    wait_();
    // rotate
    rotate( degrees2radians(angular_speed), degrees2radians(angle), clockwise);
    wait_();
    //move forward
    move (degrees2radians(forward_speed),1.0,1);
    wait_();
    // rotate
    rotate( degrees2radians(angular_speed), degrees2radians(angle), anticlockwise);
    wait_();
    //move forward
    move (degrees2radians(forward_speed),1.0,1);
    wait_();


}
void rotate ( double angular_speed, double relative_angle, bool clock_wise)
{

    geometry_msgs::Twist vel_msg;
    vel_msg.linear.x=0;
    vel_msg.linear.y=0;
    vel_msg.linear.z=0;

    vel_msg.angular.x=0;
    vel_msg.angular.y=0;

    if(clock_wise)
        vel_msg.angular.z=-abs(angular_speed);
    else
        vel_msg.angular.z=abs(angular_speed);

    double current_angle =0.0;
    double t0 = ros::Time::now().toSec();
    ros::Rate loop_rate(1000);
    int ok=0;
    float temp=0;
    cout<<"desider angle: "<<relative_angle<<endl;
    do{
        velocity_publisher.publish(vel_msg);

        double t1= ros::Time::now().toSec();


        ok++;
        if(ok==2 ) {

            //    temp = t1-1;


        }
        //  t1=t1-temp;
        current_angle= angular_speed * ((t1-t0)-temp);

        cout<<"Current_angle: "<< current_angle<<endl;

        cout<<"t1: "<< t1<<endl;

        ros::spinOnce();
        loop_rate.sleep();

    }while(current_angle<relative_angle);
    vel_msg.angular.z=0;
    velocity_publisher.publish(vel_msg);


}


void move ( double speed, double distance, bool isForward)

{
    geometry_msgs::Twist vel_msg;

    if(isForward)
        vel_msg.linear.x = abs(speed);
    else
        vel_msg.linear.x=-abs(speed);
    // vel_msg.linear.y=0;
    // vel_msg.linear.z=0;


    //  vel_msg.angular.x=0;
    //  vel_msg.angular.y=0;
    //  vel_msg.angular.z=0;


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


        cout<<"Current_distance: "<< current_distance<<endl;
        cout<<"time: "<< t1-t0<<endl;
        cout<<"time1: "<< t1<<endl;

        ros::spinOnce();
        loop_rate.sleep();


    }while(current_distance <distance);
    vel_msg.linear.x=0;
    velocity_publisher.publish(vel_msg);



}

double degrees2radians (double angle_in_degrees){
    return angle_in_degrees *PI /180.0;
}