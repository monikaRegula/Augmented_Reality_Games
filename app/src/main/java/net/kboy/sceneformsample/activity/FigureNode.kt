package net.kboy.sceneformsample.activity

import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.ux.TransformableNode
import com.google.ar.sceneform.ux.TransformationSystem
import kotlin.math.absoluteValue

class FigureNode(transformationSystem: TransformationSystem) : TransformableNode(transformationSystem) {
    var color = ""
    var figure =""

    fun addFigure(color:String,figure:String){
        this.color=color
        this.figure=figure
        if (color=="w"){
            this.localRotation = Quaternion.axisAngle(Vector3(0f, 1f, 0f), 90f)
        }else{
            this.localRotation = Quaternion.axisAngle(Vector3(0f, 1f, 0f), -90f)
        }
    }

    fun checkIfMoveIsPossible(from: List<Int>, to: List<Int>, collision:Boolean, figure:String, color:String): Boolean {
        var possible = false
        when(figure){
            "k" ->
                if((from[0]-to[0]).absoluteValue<=1 && (from[1]-to[1]).absoluteValue<=1){ possible= true }
            "p" ->
                 if(collision && color=="w" && from[1]-to[1]==-1 && (from[0]-to[0]).absoluteValue==1)
                    { possible = true }
                else if(collision && color=="b" && from[1]-to[1]==1 && (from[0]-to[0]).absoluteValue==1)
                    { possible = true }
                else if(!collision && color=="w" && from[1]-to[1]==-1 && (from[0]-to[0]).absoluteValue==0)
                    { possible = true }
                else if(!collision && color=="b" && from[1]-to[1]==1 && (from[0]-to[0]).absoluteValue==0)
                    { possible = true }
                else if(!collision && color=="w" && from[1]-to[1]==-2 && (from[0]-to[0]).absoluteValue==0  && from[1]==1)
                 { possible = true }
                 else if(!collision && color=="b" && from[1]-to[1]==2 && (from[0]-to[0]).absoluteValue==0 && from[1]==6)
                 { possible = true }
            "t"->
                if(from[0]-to[0]==0 || from[1]-to[1]==0) { possible = true }
            "h"->
                if((from[0]-to[0]).absoluteValue==1 && (from[1]-to[1]).absoluteValue==2){ possible = true }
                else if((from[0]-to[0]).absoluteValue==2 && (from[1]-to[1]).absoluteValue==1){ possible = true }

            "b"->
                if((from[0]-to[0]).absoluteValue==(from[1]-to[1]).absoluteValue){ possible = true }
            "q"->
                if(from[0]-to[0]==0 || from[1]-to[1]==0){ possible = true }
                else if((from[0]-to[0]).absoluteValue==(from[1]-to[1]).absoluteValue) { possible = true }

            else ->possible= false
        }
        return possible
    }

}
