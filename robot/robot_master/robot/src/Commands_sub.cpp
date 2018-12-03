//
// Created by andrei on 04/03/18.
//

#include "ros/ros.h"
#include "std_msgs/String.h"
#include <boost/algorithm/string.hpp>
#include "geometry_msgs/Twist.h"
#include <vector>
#include <string>
#include <tf/tf.h>
#include <geometry_msgs/Pose2D.h>
#include <nav_msgs/Odometry.h>
#include <math.h>
#include <fstream>


using namespace std;

ros::Publisher velocity_publisher;
const double PI = 3.14159265358979323846;
geometry_msgs::Pose2D current_pose;
const int N = 5;
bool isEmpty = false;

struct commands_t {
    int is_command = 0;
    int times_to_exec = 1;
    string COMM = "STOP";
    string direction = "forward";
    double angle = 90.0;
    double distance = 1;
} commands [N];


/*void odomCallback(const nav_msgs::OdometryConstPtr& msg)
{
    // linear position
    current_pose.x = msg->pose.pose.position.x;
    current_pose.y = msg->pose.pose.position.y;

    // quaternion to RPY conversion
    tf::Quaternion q(
            msg->pose.pose.orientation.x,
            msg->pose.pose.orientation.y,
            msg->pose.pose.orientation.z,
            msg->pose.pose.orientation.w);
    tf::Matrix3x3 m(q);
    double roll, pitch, yaw;
    m.getRPY(roll, pitch, yaw);

    // angular position
    current_pose.theta = yaw;
}*/
void read();
void clean_file();
void read()
{

    ifstream inFile;
    inFile.open("/home/andrei/Commands_Folder/commands.txt");
    std::string line;
    int index = 0;
    if (!inFile)
    {
        cout<<"something is wrong\n";
        return;
    }
    while (std::getline(inFile, line)) {
        std::string s;
        std::string times;
        times = line.substr(1,1);
        s = line.substr(3,line.length()-3);

        if(line == "empty"){
            isEmpty = true;
            break;
        }
        isEmpty = false;
        ROS_INFO("Reading from file");
        if (line != "") {
            cout << "Processing: " << line<<"\n";
        }


        if (s != "" && index < N) {
            commands[index].times_to_exec = std::stoi(times);
            std::string delimiter = "-";
            std::string comm = s.substr(0, s.find(delimiter));
            s.erase(0, s.find(delimiter) + delimiter.length());
            commands[index].is_command = 1;
            commands[index].COMM = comm;
            delimiter = " ";
            size_t pos = 0;
            std::string token;
            std::vector <std::string> attributes;
            while ((pos = s.find(delimiter)) != std::string::npos) {
                token = s.substr(0, pos);
                token.append(":");
                attributes.push_back(token);
                s.erase(0, pos + delimiter.length());
            }
            for (int i = 0; i < attributes.size(); i++) {
                s = attributes[i];
                delimiter = ":";
                std::string aux = "";
                size_t pos = 0;
                std::string token;
                while ((pos = s.find(delimiter)) != std::string::npos) {
                    token = s.substr(0, pos);
                    if (aux == "dir") {
                        commands[index].direction = token;
                    }
                    if (aux == "len") {
                        if(stoi(token) >= 0) {
                            commands[index].distance = stod(token);
                        }
                    }
                    if (aux == "ori") {
                        commands[index].angle = stod(token);
                    }
                    s.erase(0, pos + delimiter.length());
                    aux = token;
                }
            }
            index++;
        }
    }
    inFile.close();
}

void clean_file(){
    ofstream clean_file;
    clean_file.open("/home/andrei/Commands_Folder/commands.txt", ios::trunc);
    clean_file<<"empty";
    clean_file.close();

    for (int i = 0; i < N; i++) {
        commands[i].is_command = 0;
        commands[i].times_to_exec =1;
        commands[i].COMM = "STOP";
        commands[i].direction = "forward";
        commands[i].distance = 1;
        commands[i].angle = 90.0;
    }
}

int main(int argc, char **argv)
{
    ROS_INFO("start");

    ros::init(argc, argv, "listener");
    ros::NodeHandle n;
    //ros::Subscriber sub = n.subscribe("retriever", 1, chatterCallback);
    //ros::Subscriber sub_odometry = n.subscribe("/odom", 1, odomCallback);
    velocity_publisher = n.advertise<geometry_msgs::Twist>("/pioneer/cmd_vel",1);
    ros::Rate loop_rate(10);
    clean_file(); //clean the commands file at program start

    while(ros::ok()) { //start ros:ok loop
        read();
        string direction;
        double distance;
        double angle;
        double speed = 0.3;

    ros::spinOnce();
    loop_rate.sleep();
    if(!isEmpty) {
        for (int i = 0; i < N; i++) {
            if (commands[i].is_command == 1) {
                if (commands[i].COMM == "MOTION") {
                    for(int j=1;j<=commands[i].times_to_exec;j++) {
                        int real_time_start = 0;
                        direction = commands[i].direction;
                        distance = commands[i].distance;
                        cout << "Moving " << direction << " distance:" << distance << "\n";
                        geometry_msgs::Twist vel_msg;

                        if (direction == "forward")
                            vel_msg.linear.x = abs(speed);
                        else
                            vel_msg.linear.x = -abs(speed);


                        double t0 = ros::Time::now().toSec();
                        // cout << "time0: " << t0 << endl;
                        double current_distance = 0;
                        ros::Rate loop_rate(1000);

                        int ok = 0;
                        float temp = 0;
                        do {
                            velocity_publisher.publish(vel_msg);
                            double t1 = ros::Time::now().toSec();
                            current_distance = speed * ((t1 - t0) - (temp * 2));

                            if (ok == 0 && current_distance > 0.5) {
                                temp = current_distance - 0.5;
                                current_distance = 0.5;
                                t1 = 1;
                                ok++;

                            }

                            /*  cout << "Current_distance: " << current_distance << endl;
                              cout << "time_diff: " << t1 - t0 << endl;
                              cout << "time1: " << t1 << endl;*/

                            ros::spinOnce();
                            loop_rate.sleep();
                            if (real_time_start == 0) {
                                double t0 = ros::Time::now().toSec();
                                current_distance = speed * ((t1 - t0) - (temp * 2));
                                real_time_start++;
                            }


                        } while (current_distance < distance);
                        vel_msg.linear.x = 0;
                        velocity_publisher.publish(vel_msg);
                    }
                }
                if (commands[i].COMM == "TURN") {
                    for(int j=1;j<=commands[i].times_to_exec;j++) {
                        int real_time_start = 0;
                        direction = commands[i].direction;
                        angle = commands[i].angle;
                        cout << "Turning " << direction << " angle:" << angle << "\n";

                        geometry_msgs::Twist vel_msg;
                        vel_msg.linear.x = 0;
                        vel_msg.linear.y = 0;
                        vel_msg.linear.z = 0;

                        vel_msg.angular.x = 0;
                        vel_msg.angular.y = 0;

                        double turn_speed = 10.0; // degrees/sec

                        double angular_speed = turn_speed * PI / 180;
                        double relative_angle = angle * PI / 180;

                        if (direction == "left")
                            vel_msg.angular.z = -abs(angular_speed);
                        else
                            vel_msg.angular.z = abs(angular_speed);

                        double current_angle = 0.0;
                        double t0 = ros::Time::now().toSec();
                        ros::Rate loop_rate(10);

                        while (current_angle < relative_angle) {
                            velocity_publisher.publish(vel_msg);
                            double t1 = ros::Time::now().toSec();
                            current_angle = angular_speed * (t1 - t0);

                            ros::spinOnce();
                            loop_rate.sleep();
                            if (real_time_start == 0) {
                                double t0 = ros::Time::now().toSec();
                                real_time_start++;
                            }
                        }


                        /* do {
                             velocity_publisher.publish(vel_msg);

                             double t1 = ros::Time::now().toSec();
                             current_angle = speed * (t1 - t0);

                             ros::spinOnce();
                             loop_rate.sleep();
                             if (real_time_start == 0) {
                                 double t0 = ros::Time::now().toSec();
                                 real_time_start++;
                             }

                         } while (current_angle < ((angle * PI) / 180));*/
                        vel_msg.angular.z = 0;
                        velocity_publisher.publish(vel_msg);
                    }
                }

                if (commands[i].COMM == "STOP") {
                    cout << "STOP\n";

                    geometry_msgs::Twist vel_msg;
                    vel_msg.linear.x = 0;
                    vel_msg.linear.y = 0;
                    vel_msg.linear.z = 0;

                    vel_msg.angular.x = 0;
                    vel_msg.angular.y = 0;

                    velocity_publisher.publish(vel_msg);

                }
            }
        }
        clean_file();
    }

    } //end of ros::ok
    return 0;
}