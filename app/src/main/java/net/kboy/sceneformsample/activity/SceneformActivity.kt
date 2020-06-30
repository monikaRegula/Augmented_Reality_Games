package net.kboy.sceneformsample.activity

import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.*
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import kotlinx.android.synthetic.main.activity_sceneform.*
import kotlinx.android.synthetic.main.scoreboard_view.view.*
import net.kboy.sceneformsample.R
import kotlin.math.absoluteValue

class SceneformActivity : AppCompatActivity() {
    private lateinit var scene: ArFragment
    private var locked = false
    private var boardObject = Uri.parse("board.sfb")
    private var gridOfNodes = Array(8) { arrayOfNulls<FigureNode>(8) }
    private var whiteRenderables = arrayOfNulls<ModelRenderable?>(6)
    private var blackRenderables = arrayOfNulls<ModelRenderable?>(6)
    private var from = listOf<Int>(-1, -1)
    private var buttonRenderable: ModelRenderable? = null
    private var firstPlayer = true
    private lateinit var anchorNode: AnchorNode
    private var scoreboardRenderable: ViewRenderable? = null
    private lateinit var scoreboard: ScoreboardView
    private lateinit var transparentMaterial : Material


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sceneform)
        scoreboard = ScoreboardView(this)
        scene = sceneformFragment.let { it as ArFragment }

        //preparing 3D models
        prepareRenderableObject()
        prepareRenderableObject(Uri.parse("towerWhite.sfb"), Uri.parse("towerBlack.sfb"), 0)
        prepareRenderableObject(Uri.parse("horseWhite.sfb"), Uri.parse("horseBlack.sfb"), 1)
        prepareRenderableObject(Uri.parse("bishopWhite.sfb"), Uri.parse("bishopBlack.sfb"), 2)
        prepareRenderableObject(Uri.parse("kingWhite.sfb"), Uri.parse("kingBlack.sfb"), 3)
        prepareRenderableObject(Uri.parse("queenWhite.sfb"), Uri.parse("queenBlack.sfb"), 4)
        prepareRenderableObject(Uri.parse("pawnWhite.sfb"), Uri.parse("pawnBlack.sfb"), 5)
        prepareRenderableObject(Uri.parse("button.sfb"))

        scene.setOnTapArPlaneListener { hitResult: HitResult, plane: Plane, motionEvent: MotionEvent ->
            if (!locked) {
                if (plane.type != Plane.Type.HORIZONTAL_UPWARD_FACING) {
                    return@setOnTapArPlaneListener
                }
                if (scene.arSceneView.scene.children.size > 2) {
                    firstPlayer = true
                    for (i in scene.arSceneView.scene.children.indices) {
                        for (j in scene.arSceneView.scene.children[i].children.indices) {
                            scene.arSceneView.scene.children[i].children[j].renderable = null
                        }
                    }
                }
                buttonRenderable!!.isShadowCaster = false
                buttonRenderable!!.material = transparentMaterial
                placeObject(scene, hitResult, boardObject)
                prepareFiguresGrid(hitResult)
                scoreboard.notification.setText("Hello in this magnificent game!!!")
            }
        }
    }

    fun onSwitchButtonClicked(view: View) {
        locked = board.isChecked
        scoreboard.notification.setText("Your board is now locked!!!You can start the game!!!")
    }

    private fun prepareFiguresGrid(hitResult: HitResult) {
        val anchor = hitResult.createAnchor()
        val anchorNode = AnchorNode(anchor)
        scene.arSceneView.scene.addChild(anchorNode)
        gridOfNodes.matrixIndices { col, row ->
            FigureNode(scene.transformationSystem).apply {
                setParent(anchorNode)
                if (row == 0) {
                    if (col == 0 || col == 7) {
                        renderable = whiteRenderables[0]
                        addFigure("w", "t")
                    } else if (col == 1 || col == 6) {
                        renderable = whiteRenderables[1]
                        addFigure("w", "h")
                    } else if (col == 2 || col == 5) {
                        renderable = whiteRenderables[2]
                        addFigure("w", "b")
                    } else if (col == 3) {
                        renderable = whiteRenderables[4]
                        addFigure("w", "q")
                    } else if (col == 4) {
                        renderable = whiteRenderables[3]
                        addFigure("w", "k")
                    }

                } else if (row == 1) {
                    renderable = whiteRenderables.last()
                    addFigure("w", "p")
                } else if (row == 7) {
                    if (col == 0 || col == 7) {
                        renderable = blackRenderables[0]
                        addFigure("b", "t")
                    } else if (col == 1 || col == 6) {
                        renderable = blackRenderables[1]
                        addFigure("b", "h")
                    } else if (col == 2 || col == 5) {
                        renderable = blackRenderables[2]
                        addFigure("b", "b")
                    } else if (col == 3) {
                        renderable = blackRenderables[3]
                        addFigure("b", "k")
                    } else if (col == 4) {
                        renderable = blackRenderables[4]
                        addFigure("b", "q")
                    }
                } else if (row == 6) {
                    renderable = blackRenderables.last()
                    addFigure("b", "p")
                } else {
                    renderable = buttonRenderable
                }
                rotationController.isEnabled = false
                scaleController.isEnabled = false
                translationController.isEnabled = false
                localPosition = Vector3(-0.35F + col * 0.1F, 0F, 0.35F - row * 0.1F);
                gridOfNodes[col][row] = this

                //adding listener to every figure
                this.setOnTapListener { _, _ ->
                    //check if the chessboard is locked with Switch button
                        if (locked) {
                        //check if first click was figure and second empty element of chessboard
                        if (this.renderable == buttonRenderable && from != listOf(-1, -1)) {
                            if (!checkIfThereIsCollision(from, listOf(col, row), gridOfNodes[from[0]][from[1]]!!.figure)) {
                                makeMovingSound()
                                if (this.checkIfMoveIsPossible(from, listOf(col, row), false, gridOfNodes[from[0]][from[1]]!!.figure, gridOfNodes[from[0]][from[1]]!!.color)) {
                                    this.renderable = gridOfNodes[from[0]][from[1]]!!.renderable
                                    this.addFigure(gridOfNodes[from[0]][from[1]]!!.color, gridOfNodes[from[0]][from[1]]!!.figure)
                                    gridOfNodes[from[0]][from[1]]!!.renderable = buttonRenderable
                                    gridOfNodes[from[0]][from[1]]!!.addFigure("", "")
                                    var vector = gridOfNodes[from[0]][from[1]]!!.localPosition
                                    vector.y = 0F
                                    gridOfNodes[from[0]][from[1]]!!.localPosition = vector
                                    from != listOf(-1, -1)
                                    firstPlayer = !firstPlayer
                                }
                                from != listOf(-1, -1)
                                var vector = gridOfNodes[from[0]][from[1]]!!.localPosition
                                vector.y = 0F
                                gridOfNodes[from[0]][from[1]]!!.localPosition = vector
                            }
                            from != listOf(-1, -1)
                            var vector = gridOfNodes[from[0]][from[1]]!!.localPosition
                            vector.y = 0F
                            gridOfNodes[from[0]][from[1]]!!.localPosition = vector
                        } else { // figure was selected
                            if (this.color == "w" && firstPlayer || this.color == "b" && !firstPlayer) { //player figure
                                //castling - changing king and tower places with each other
                                if ((this.figure == "t" && gridOfNodes[from[0]][from[1]]!!.figure == "k") || (this.figure == "k" && gridOfNodes[from[0]][from[1]]!!.figure == "t")) {
                                    //white figures
                                    if (firstPlayer && gridOfNodes[5][0]!!.renderable == buttonRenderable && gridOfNodes[6][0]!!.renderable == buttonRenderable) {
                                        makeMovingSound()
                                        gridOfNodes[5][0]!!.renderable = whiteRenderables[0]
                                        gridOfNodes[6][0]!!.renderable = whiteRenderables[3]
                                        this.renderable = buttonRenderable
                                        gridOfNodes[4][0]!!.renderable = buttonRenderable
                                        gridOfNodes[7][0]!!.addFigure("", "")
                                        gridOfNodes[4][0]!!.addFigure("", "")
                                        gridOfNodes[5][0]!!.addFigure("w", "t")
                                        gridOfNodes[6][0]!!.addFigure("w", "k")
                                        from != listOf(-1, -1)
                                        firstPlayer = !firstPlayer
                                        var vector = gridOfNodes[from[0]][from[1]]!!.localPosition
                                        vector.y = 0F
                                        gridOfNodes[from[0]][from[1]]!!.localPosition = vector
                                    } else if (!firstPlayer && gridOfNodes[1][7]!!.renderable == buttonRenderable && gridOfNodes[2][7]!!.renderable == buttonRenderable) {
                                        //black figures
                                        makeMovingSound()
                                        gridOfNodes[2][7]!!.renderable = blackRenderables[0]
                                        gridOfNodes[1][7]!!.renderable = blackRenderables[3]
                                        this.renderable = buttonRenderable
                                        gridOfNodes[3][7]!!.renderable = buttonRenderable
                                        gridOfNodes[0][7]!!.addFigure("", "")
                                        gridOfNodes[3][7]!!.addFigure("", "")
                                        gridOfNodes[2][7]!!.addFigure("w", "t")
                                        gridOfNodes[1][7]!!.addFigure("w", "k")
                                        from != listOf(-1, -1)
                                        firstPlayer = !firstPlayer
                                        var vector = gridOfNodes[from[0]][from[1]]!!.localPosition
                                        vector.y = 0F
                                        gridOfNodes[from[0]][from[1]]!!.localPosition = vector
                                    }

                                } else {
                                    // user takes other figure to move, changed his mind
                                    from = listOf(col, row)
                                    for (i in 0..7) {
                                        for (j in 0..7) {
                                            var vector = gridOfNodes[i][j]!!.localPosition
                                            vector.y = 0F
                                            gridOfNodes[i][j]!!.localPosition = vector
                                        }
                                    }
                                    //figures after tapping goes up to show which one was chosen
                                    var vector = this.localPosition
                                    vector.y = 0.1F
                                    this.localPosition = vector
                                }

                            } else if (from != listOf(-1, -1)) { // capturing figure (destroying opponent figure)
                                if (!checkIfThereIsCollision(from, listOf(col, row), gridOfNodes[from[0]][from[1]]!!.figure)) {
                                    if (this.checkIfMoveIsPossible(from, listOf(col, row), true, gridOfNodes[from[0]][from[1]]!!.figure, gridOfNodes[from[0]][from[1]]!!.color)) {
                                        makeMovingSound()
                                        if (this.figure == "k") {
                                            win()
                                        }
                                        this.renderable = gridOfNodes[from[0]][from[1]]!!.renderable
                                        this.addFigure(gridOfNodes[from[0]][from[1]]!!.color, gridOfNodes[from[0]][from[1]]!!.figure)
                                        gridOfNodes[from[0]][from[1]]!!.renderable = buttonRenderable
                                        gridOfNodes[from[0]][from[1]]!!.addFigure("", "")
                                        var vector = gridOfNodes[from[0]][from[1]]!!.localPosition
                                        vector.y = 0F
                                        gridOfNodes[from[0]][from[1]]!!.localPosition = vector
                                        from != listOf(-1, -1)
                                        firstPlayer = !firstPlayer
                                    }
                                }
                            } else {
                                //figure goes down because user wanted to make impossible move
                                from != listOf(-1, -1)
                                var vector = gridOfNodes[from[0]][from[1]]!!.localPosition
                                vector.y = 0F
                                gridOfNodes[from[0]][from[1]]!!.localPosition = vector
                            }
                        }
                    }

                }
            }
        }
    }

    private fun win() {
        locked = false
        if (firstPlayer) {
            scoreboard.notification.setText("White wins!!!")
        } else {
            scoreboard.notification.setText("Black wins!!!")
        }
        var victorySound = MediaPlayer.create(this@SceneformActivity, R.raw.ta_da)
        victorySound.start()
    }

    fun checkIfThereIsCollision(from: List<Int>, to: List<Int>, figure: String): Boolean {
        var collision = false
        if (figure == "t" || figure == "q") {
            if (from[0] - to[0] == 0) {
                if (from[1] < to[1]) {
                    for (i in from[1] + 1..to[1]) {
                        if (gridOfNodes[from[0]][i]!!.renderable != buttonRenderable) {
                            return true
                        }
                    }
                } else {
                    for (i in to[1]..from[1] - 1) {
                        if (gridOfNodes[from[0]][i]!!.renderable != buttonRenderable) {
                            return true
                        }
                    }
                }

            } else if (from[1] - to[1] == 0) {
                if (from[0] < to[0]) {
                    for (i in from[0] + 1..to[0]-1) {
                        if (gridOfNodes[i][from[1]]!!.renderable != buttonRenderable) {
                            return true
                        }
                    }
                } else {
                    for (i in to[0]..from[0] - 1) {
                        if (gridOfNodes[i][from[1]]!!.renderable != buttonRenderable) {
                            return true
                        }
                    }
                }

            }


        } else if (figure == "b" || figure == "q") {
            if ((from[0] - to[0]).absoluteValue == (from[1] - to[1]).absoluteValue) {
                if (from[0] < to[0] && from[1] < to[1]) {
                    for (i in 1..to[0] - from[0]) {
                        if (gridOfNodes[from[0] + i][from[1] + i]!!.renderable != buttonRenderable) {
                            return true
                        }
                    }
                } else if (from[0] < to[0] && from[1] > to[1]) {
                    for (i in 1..to[0] - from[0]) {
                        if (gridOfNodes[from[0] + i][from[1] - i]!!.renderable != buttonRenderable) {
                            return true
                        }
                    }
                } else if (from[0] > to[0] && from[1] > to[1]) {
                    for (i in 1..from[0] - to[0]) {
                        if (gridOfNodes[from[0] - i][from[1] - i]!!.renderable != buttonRenderable) {
                            return true
                        }
                    }
                } else if (from[0] > to[0] && from[1] < to[1]) {
                    for (i in 1..from[0] - to[0]) {
                        if (gridOfNodes[from[0] - i][from[1] + i]!!.renderable != buttonRenderable) {
                            return true
                        }
                    }
                }


            }

        }
        return collision
    }

    fun <T> Array<Array<T>>.matrixIndices(f: (Int, Int) -> Unit) {
        this.forEachIndexed { col, array ->
            array.forEachIndexed { row, _ ->
                f(col, row)
            }
        }
    }

    private fun placeObject(fragment: ArFragment, hitResult: HitResult, model: Uri) {
        ModelRenderable.builder()
                .setSource(fragment.context, model)
                .build()
                .thenAccept {
                    addNodeToScene(fragment, hitResult, it)
                }
                .exceptionally {
                    return@exceptionally null
                }
    }

    private fun prepareRenderableObject() {
        ViewRenderable.builder()
                .setView(this, scoreboard)
                .build()
                .thenAccept {
                    it.isShadowReceiver = true
                    scoreboardRenderable = it
                }
    }


    private fun addNodeToScene(fragment: ArFragment, hitResult: HitResult, renderable: Renderable) {
        val anchor = hitResult.createAnchor()
        val anchorNode = AnchorNode(anchor)
        this.anchorNode = anchorNode
        var node = TransformableNode(fragment.transformationSystem);
        node.renderable = renderable;
        node.localPosition = Vector3(0F, -0.03F, 0F);
        node.rotationController.isEnabled = false
        node.scaleController.isEnabled = false
        node.translationController.isEnabled = false
        anchorNode.addChild(node)
        fragment.arSceneView.scene.addChild(anchorNode)
        // Add the scoreboard view to the plane
        val renderableView = scoreboardRenderable
        TransformableNode(fragment.transformationSystem)
                .also {
                    it.setParent(anchorNode)
                    it.renderable = renderableView
                    it.localPosition = Vector3(0F, 1F, 0F)
                }
    }

    private fun prepareRenderableObject(model: Uri, modelBlack: Uri, number: Int) {
        ModelRenderable.builder()
                .setSource(this, model)
                .build()
                .thenAccept {
                    Log.e("SDS", "diz")
                    whiteRenderables[number] = it
                }
        ModelRenderable.builder()
                .setSource(this, modelBlack)
                .build()
                .thenAccept { blackRenderables[number] = it }
    }

    private fun prepareRenderableObject(model: Uri) {
                MaterialFactory.makeTransparentWithColor(
                this,
                Color(0.0f,0.0f,0.0f,0.0f)) // Blue color with Alpha 0.5
                .thenAccept {
                    transparentMaterial = it
                }
        ModelRenderable.builder()
                .setSource(this, model)
                .build()
                .thenAccept {
                    Log.e("SDS", "diz")
                    buttonRenderable = it
                }
    }

    private fun placeObject(fragment: ArFragment, move: Vector3, model: Uri, hitResult: HitResult, number: Int) {
        ModelRenderable.builder()
                .setSource(this, model)
                .build()
                .thenAccept { whiteRenderables[number] = it }
        ModelRenderable.builder()
                .setSource(this, model)
                .build()
                .thenAccept { blackRenderables[number] = it }
    }

    private fun makeMovingSound() {
        val sound_move_chees = MediaPlayer.create(this@SceneformActivity, R.raw.move_figure)
        sound_move_chees.start()
    }

}
