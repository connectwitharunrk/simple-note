package com.arunrk.simplenote.di

import com.arunrk.simplenote.data.remote.NoteApi
import com.arunrk.simplenote.data.repository.NoteRepositoryImpl
import com.arunrk.simplenote.domain.repository.NoteRepository
import com.arunrk.simplenote.domain.usecase.CreateNoteUseCase
import com.arunrk.simplenote.domain.usecase.DeleteNoteUseCase
import com.arunrk.simplenote.domain.usecase.GetNoteUseCase
import com.arunrk.simplenote.domain.usecase.GetNotesUseCase
import com.arunrk.simplenote.domain.usecase.SetNoteArchivedUseCase
import com.arunrk.simplenote.domain.usecase.SetNotePinnedUseCase
import com.arunrk.simplenote.domain.usecase.UpdateNoteUseCase
import com.arunrk.simplenote.network.ApiConfig
import com.arunrk.simplenote.network.createNoteHttpClient
import com.arunrk.simplenote.network.defaultBaseUrl
import com.arunrk.simplenote.presentation.editor.NoteEditorViewModel
import com.arunrk.simplenote.presentation.notes.NotesListViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Wiring, split by layer so the dependency direction is visible in the graph itself.
 *
 * Note the single `single<NoteRepository> { NoteRepositoryImpl(...) }` binding: everything
 * above the data layer asks for the interface and can never accidentally reach the Ktor
 * implementation. That is the one binding that makes the architecture hold at runtime rather
 * than only on paper.
 *
 * @param baseUrl overrides the platform default — used to point a physical device at a
 *   machine on the LAN, or a build at a staging server.
 */
fun dataModule(baseUrl: String? = null): Module = module {
    single { ApiConfig(baseUrl ?: defaultBaseUrl()) }
    single { createNoteHttpClient(enableLogging = ENABLE_HTTP_LOGGING) }
    single { NoteApi(httpClient = get(), apiConfig = get()) }
    single<NoteRepository> { NoteRepositoryImpl(noteApi = get()) }
}

/**
 * Use cases are `factory` rather than `single`: they are stateless and cheap, and creating one
 * per injection avoids any temptation to keep state in them later.
 */
val domainModule: Module = module {
    factory { GetNotesUseCase(get()) }
    factory { GetNoteUseCase(get()) }
    factory { CreateNoteUseCase(get()) }
    factory { UpdateNoteUseCase(get()) }
    factory { DeleteNoteUseCase(get()) }
    factory { SetNotePinnedUseCase(get()) }
    factory { SetNoteArchivedUseCase(get()) }
}

val presentationModule: Module = module {
    viewModelOf(::NotesListViewModel)
    viewModelOf(::NoteEditorViewModel)
}

fun appModules(baseUrl: String? = null): List<Module> =
    listOf(dataModule(baseUrl), domainModule, presentationModule)

/** Ktor request logging is noisy and can echo note contents, so it is off by default. */
private const val ENABLE_HTTP_LOGGING = false
