package dev.fritz2.identification

import dev.fritz2.lenses.IdProvider
import dev.fritz2.lenses.Lens
import dev.fritz2.lenses.elementLens
import dev.fritz2.lenses.positionLens

/**
 *  gives you the a new [RootInspector] as starting point.
 */
fun <T> inspect(data: T, id: String = "") = RootInspector<T>(data, id)

/**
 * represents the data and corresponding id of certain value
 * in a deep nested model structure.
 *
 * @property data [T] representation of stored data
 * @property id [String] representation of the corresponding id
 */
interface Inspector<T> {
    val data: T
    val id: String

    /**
     * creates a new [Inspector] for a part of your underlying data-model
     *
     * @param lens a [Lens] describing, of which part of your data
     * model you want to have the next [Inspector]
     */
    fun <X> sub(lens: Lens<T, X>): Inspector<X>
}


/**
 * [RootInspector] is the starting point for getting your data and corresponding ids from your
 * deep nested model structure. Get this by calling the factory method [inspect].
 *
 * [Inspector] is useful in validation process to know which html elements
 * (when they are rendered with an store.id) are not valid.
 */
class RootInspector<T>(
    override val data: T,
    override val id: String = ""
) : Inspector<T> {

    override fun <X> sub(lens: Lens<T, X>): SubInspector<T, T, X> =
        SubInspector(this, lens, this, lens)
}

/**
 *  [SubInspector] is the next lower [Inspector] in a deep nested model structure.
 *  It's generated by calling the [sub] function on an [Inspector].
 */
class SubInspector<R, P, T>(
    private val parent: Inspector<P>,
    private val lens: Lens<P, T>,
    val rootModelId: RootInspector<R>,
    val rootLens: Lens<R, T>
) : Inspector<T> {

    /**
     * generates the corresponding id
     */
    override val id: String by lazy { "${parent.id}.${lens.id}".trimEnd('.') }

    /**
     * returns the underlying data
     */
    override val data: T = lens.get(parent.data)

    override fun <X> sub(lens: Lens<T, X>): SubInspector<R, T, X> =
        SubInspector(this, lens, rootModelId, this.rootLens + lens)
}

/**
 * creates a [Inspector] for an element in your [Inspector]'s list.
 *
 * @param element to get the [Inspector] for
 * @param idProvider to get the id from an instance
 */
inline fun <reified T, I> RootInspector<List<T>>.sub(
    element: T,
    noinline idProvider: IdProvider<T, I>
): SubInspector<List<T>, List<T>, T> {
    val lens = elementLens(element, idProvider)
    return SubInspector(this, lens, this, lens)
}

/**
 * creates a [Inspector] for an element in your [Inspector]'s list.
 *
 * @param index you need the [Inspector] for
 */
inline fun <reified X> RootInspector<List<X>>.sub(index: Int): SubInspector<List<X>, List<X>, X> {
    val lens = positionLens<X>(index)
    return SubInspector(this, lens, this, lens)
}

/**
 * creates a [Inspector] for an element in your [Inspector]'s list.
 *
 * @param element to get the [Inspector] for
 * @param idProvider to get the id from an instance
 */
inline fun <R, P, reified T, I> SubInspector<R, P, List<T>>.sub(
    element: T,
    noinline idProvider: IdProvider<T, I>
): SubInspector<R, List<T>, T> {
    val lens = elementLens(element, idProvider)
    return SubInspector<R, List<T>, T>(this, lens, this.rootModelId, this.rootLens + lens)
}

/**
 * creates a [Inspector] for an element in your [Inspector]'s list.
 *
 * @param index of the element in your list you need the [Inspector] for
 */
inline fun <R, P, reified X> SubInspector<R, P, List<X>>.sub(index: Int): SubInspector<R, List<X>, X> {
    val lens = positionLens<X>(index)
    return SubInspector<R, List<X>, X>(this, lens, this.rootModelId, this.rootLens + lens)
}
