@file:Suppress("DEPRECATION")

package cc.aoeiuv020.comic.ui

import android.app.Activity
import android.app.ProgressDialog
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AlertDialog
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import cc.aoeiuv020.comic.R
import cc.aoeiuv020.comic.api.ComicGenre
import cc.aoeiuv020.comic.api.ComicSite
import cc.aoeiuv020.comic.presenter.MainPresenter
import cc.aoeiuv020.comic.ui.base.MainBaseNavigationActivity
import com.miguelcatalan.materialsearchview.MaterialSearchView
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.nav_header_main.view.*
import kotlinx.android.synthetic.main.site_list_item.view.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.browse
import org.jetbrains.anko.debug


/**
 * 主页，展示网站，分类，
 * Created by AoEiuV020 on 2017.09.12-19:04:44.
 */

class MainActivity : MainBaseNavigationActivity(), AnkoLogger {
    private val alertDialog: AlertDialog by lazy { AlertDialog.Builder(this).create() }
    private val progressDialog: ProgressDialog by lazy { ProgressDialog(this) }
    private var url: String = "https://github.com/AoEiuV020/comic"
    private lateinit var presenter: MainPresenter
    private lateinit var genres: List<ComicGenre>
    private var site: ComicSite? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        searchView.setOnQueryTextListener(object : MaterialSearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                // 收起软键盘，
                searchView.hideKeyboard(searchView)
                site?.also {
                    loading(progressDialog, R.string.search_result)
                    presenter.search(it, query)
                } ?: run {
                    debug { "没有选择网站，先弹出网站选择，" }
                    presenter.requestSites()
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean = false

        })

        presenter = MainPresenter(this)
        presenter.start()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        val item = menu.findItem(R.id.action_search)
        searchView.setMenuItem(item)
        menu.findItem(R.id.browse).setOnMenuItemClickListener {
            browse(url)
        }
        return true
    }

    private val GROUP_ID: Int = 1

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.groupId) {
            GROUP_ID -> {
                showGenre(genres[item.order])
            }
            else -> when (item.itemId) {
                R.id.select_sites -> presenter.requestSites()
                R.id.settings -> {
                    Snackbar.make(drawer_layout, "没实现", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
        closeDrawer()
        return true
    }

    fun showUrl(url: String) {
        this.url = url
    }

    fun showGenre(genre: ComicGenre) {
        title = genre.name
        url = genre.url
        progressDialog.dismiss()
        (fragment_container as ComicListFragment).showGenre(genre)
        closeDrawer()
    }

    fun showSites(sites: List<ComicSite>) {
        AlertDialog.Builder(this@MainActivity).setAdapter(SiteListAdapter(this@MainActivity, sites)) { _, index ->
            val site = sites[index]
            this.site = site
            debug { "选中网站：${site.name}，弹出侧栏，" }
            showSite(site)
        }.show()
    }

    fun showSite(site: ComicSite) {
        this.site = site
        url = site.baseUrl
        openDrawer()
        loading(progressDialog, R.string.genre_list)
        nav_view.getHeaderView(0).apply {
            selectedSiteName.text = site.name
            glide {
                it.load(site.logo).holdInto(selectedSiteLogo)
            }
        }
        presenter.requestGenres(site)
    }

    fun showGenres(genres: List<ComicGenre>) {
        this.genres = genres
        progressDialog.dismiss()
        nav_view.menu.run {
            removeGroup(GROUP_ID)
            genres.forEachIndexed { index, (name) ->
                add(GROUP_ID, index, index, name)
            }
        }
    }

    fun showError(message: String, e: Throwable) {
        progressDialog.dismiss()
        alertError(alertDialog, message, e)
    }
}

class SiteListAdapter(val ctx: Activity, private val sites: List<ComicSite>) : BaseAdapter() {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: View.inflate(ctx, R.layout.site_list_item, null)
        val site = getItem(position)
        view.apply {
            siteName.text = site.name
            ctx.glide {
                it.load(site.logo).into(siteLogo)
            }
        }
        return view
    }

    override fun getItem(position: Int) = sites[position]

    override fun getItemId(position: Int) = 0L

    override fun getCount() = sites.size
}
