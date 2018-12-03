//
// Created by andrei on 07/03/18.
//

//
// Created by andrei on 04/03/18.
//

#include "ros/ros.h"
#include "std_msgs/String.h"
#include <boost/algorithm/string.hpp>
#include "geometry_msgs/Twist.h"
//#include "Commands.h"
#include <vector>
#include <string>

using namespace std;

ros::Publisher velocity_publisher;
const double PI = 3.14159265358979323846;

class Commands {
public:
    void move(double distance, string direction);
    void turn(string direction, double angle);
    Commands();  // This is the constructor

private:
    double length;
};

Commands::Commands(void){
}

void Commands::move(double distance, string direction) {
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

    int real_start_time =0;
    int ok=0;
    float temp=0;
    do{
        velocity_publisher.publish(vel_msg);
        double t1=ros::Time::now().toSec();
        current_distance = 1 * ((t1-t0)-(temp*2));

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
        if(real_start_time == 0){
            real_start_time++;
            t0 = ros::Time::now().toSec();
        }


    }while(current_distance <distance);
    vel_msg.linear.x=0;
    velocity_publisher.publish(vel_msg);

}

void Commands::turn(string direction, double angle) {
    geometry_msgs::Twist vel_msg;
    vel_msg.linear.x=0;
    vel_msg.linear.y=0;
    vel_msg.linear.z=0;

    vel_msg.angular.x=0;
    vel_msg.angular.y=0;

    if(direction == "right")
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


void chatterCallback(const std_msgs::String::ConstPtr& msg)
{
    int distance = 1;
    std::string direction = "forward";
    double orientation = 90.0;

    Commands commands;
    ROS_INFO("I heard: [%s]", msg->data.c_str());
    std::string s = msg->data;
    if(s != ""){
        std::string delimiter = "-";
        std::string comm = s.substr(0, s.find(delimiter));
        s.erase(0, s.find(delimiter) + delimiter.length());
        delimiter = " ";

        size_t pos = 0;
        std::string token;
        std::vector <std::string> attributes;
        while ((pos = s.find(delimiter)) != std::string::npos) {
            token = s.substr(0, pos);
            attributes.push_back(token);
            s.erase(0, pos + delimiter.length());
        }
        for (int i=0; i<attributes.size(); i++) {
            s = attributes[i];
            delimiter = ":";
            std::string aux = "";
            size_t pos = 0;
            std::string token;
            while ((pos = s.find(delimiter)) != std::string::npos) {
                token = s.substr(0, pos);
                if(aux == "dir"){
                    direction = token;
                }
                if(aux == "len"){
                    distance = stoi(token);
                }
                if(aux == "ori"){
                    orientation = stod(token);
                }
                vector <string> attributes;
                attributes.push_back(token);
                s.erase(0, pos + delimiter.length());
                aux = token;
            }
        }

        if(comm == "MOTION"){
            commands.move(distance,direction);
        }
        if(comm == "TURN"){
            commands.turn(direction, orientation);
        }

    }

}

int main(int argc, char **argv)
{
    ROS_INFO("start");

    ros::init(argc, argv, "listener");
    ros::NodeHandle n;
    ros::Subscriber sub = n.subscribe("retriever", 1, chatterCallback);
    ros::Subscriber sub_odometry = n.subscribe("/odom", 1, odomCallback);
    ros::Publisher movement_pub = n.advertise<geometry_msgs::Twist>("/pioneer/cmd_vel",1);
    ros::spin();

    return 0;
}