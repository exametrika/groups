set ttymouse=xterm2
set ttyfast
set clipboard=unnamed
set cursorline
"set cursorcolumn
set autoread
set autowriteall
set ignorecase
"set undofile
set virtualedit=all
syntax on
filetype plugin indent on
set mouse=a
set number
set tabstop=4 shiftwidth=4 expandtab
set cc=80
hi ColorColumn ctermbg=lightgrey
set background=dark
set foldmethod=syntax
set fdls=99
set foldtext=""
set fillchars="fold: "

execute pathogen#infect()
Helptags

" IndentLine
let g:indentLine_color_term = 235
let g:indentLine_char = '│'

" NERDTree
nmap <Leader>f :NERDTreeTabsToggle<CR>               
nmap <Leader>s :NERDTreeTabsFind<CR>               
nmap <Leader>cd :NERDTreeCWD<CR>               
let g:NERDTreeDirArrowExpandable="+"
let g:NERDTreeDirArrowCollapsible="-"
let NERDTreeShowHidden=1

" NERDTreeTabs
"let g:nerdtree_tabs_open_on_console_startup = 1

"TagBar
nmap <Leader>t :TagbarToggle<CR>
nmap <Leader>o :TagbarOpenAutoClose<CR>
" let g:tagbar_left = 1
let g:tagbar_sort = 0
let g:tagbar_iconchars = ['+', '-']
autocmd FileType tagbar setlocal nocursorline nocursorcolumn

" Eclim
nmap <f5> :ProjectRefreshAll<CR>
nmap <f3> :JavaSearch<CR>
nmap <c-f3> :JavaSearchContext<CR>
imap <c-1> :JavaCorrect<CR>
nmap <f4> :JavaHierarchy<CR>
nmap <f2> :JavaDocPreview<CR>
imap <c-f2> :JavaDocComment<CR>
nmap <f8> :JavaDebugStart<CR>
nmap <c-f8> :JavaDebugStop<CR>
nmap <f9> :JavaDebugBreakpointToggle<CR>
nmap <f5> :JavaDebugStep into<CR>
nmap <f6> :JavaDebugStep over<CR>
nmap <f7> :JavaDebugStep return<CR>
let g:EclimCompletionMethod = 'omnifunc'
let g:EclimLocateFileScope='workspace'
let g:EclimLocateFileCaseInsensitive='always'

" CtrlP
nmap <c-m> :CtrlPMRU<CR>

" NERDCommenter
let g:NERDDefaultAlign = 'left'

" vim-airline
let g:airline#extensions#tabline#enabled = 1
let g:airline_theme='solarized'

" UltiSnips
let g:UltiSnipsExpandTrigger="<c-j>"
let g:UltiSnipsListSnippets = '<c-l>'

" YouCompleteMe
set completeopt-=preview
"let g:ycm_autoclose_preview_window_after_insertion = 1

" vimwiki
let g:vimwiki_list = [{'path': '~/workspace/pm', 'auto_tags': 1}]
let g:vimwiki_folding='syntax'
let g:tagbar_type_vimwiki = {'ctagstype':'vimwiki', 'kinds':['h:header'], 'sro':'&&&', 'kind2scope':{'h':'header'}
    \ , 'sort':0, 'ctagsbin':'~/.vim/bundle/vimwiki/vwtags.py', 'ctagsargs': 'default'}

" vim-colors-solarized 
"let g:solarized_termcolors=256
colorscheme solarized

hi Folded term=bold cterm=bold ctermfg=grey

" cmdalias_vim
augroup VIMRC_aliases
  au!
  au VimEnter * CmdAlias pre ProjectRefresh
  au VimEnter * CmdAlias pra ProjectRefreshAll
  au VimEnter * CmdAlias pp ProjectProblems
  au VimEnter * CmdAlias pb ProjectBuild
  au VimEnter * CmdAlias pru ProjectRun
  au VimEnter * CmdAlias pcd ProjectCD
  au VimEnter * CmdAlias plcd ProjectLCD
  au VimEnter * CmdAlias pgrep ProjectGrep
  au VimEnter * CmdAlias plgrep ProjectLGrep
  au VimEnter * CmdAlias ptodo ProjectTodo
  au VimEnter * CmdAlias todo Todo
  au VimEnter * CmdAlias loc LocateFile
  au VimEnter * CmdAlias si Sign
  au VimEnter * CmdAlias ss Signs
  au VimEnter * CmdAlias jc JavaCorrect
  au VimEnter * CmdAlias js JavaSearch
  au VimEnter * CmdAlias jsc JavaSearchContext
  au VimEnter * CmdAlias jh JavaHierarchy
  au VimEnter * CmdAlias jf JavaFormat
  au VimEnter * CmdAlias %jf %JavaFormat
  au VimEnter * CmdAlias jdc JavaDocComment
  au VimEnter * CmdAlias jdp JavaDocPreview
  au VimEnter * CmdAlias jds JavaDocSearch
  au VimEnter * CmdAlias ja Java
  au VimEnter * CmdAlias ju JavaUnit
  au VimEnter * CmdAlias jur JavaUnitResult
  au VimEnter * CmdAlias ant Ant
  au VimEnter * CmdAlias jps Jps
  au VimEnter * CmdAlias jrn JavaNew
  au VimEnter * CmdAlias jrc JavaConstructor
  au VimEnter * CmdAlias jrg JavaGet
  au VimEnter * CmdAlias jrs JavaSet
  au VimEnter * CmdAlias jrgs JavaGetSet
  au VimEnter * CmdAlias jri JavaImpl
  au VimEnter * CmdAlias jrd JavaDelegate
  au VimEnter * CmdAlias ji JavaImport
  au VimEnter * CmdAlias jo JavaImportOrganize
  au VimEnter * CmdAlias jrr JavaRename
  au VimEnter * CmdAlias jrm JavaMove
  au VimEnter * CmdAlias jru RefactorUndo
  au VimEnter * CmdAlias jrup RefactorUndoPeek
  au VimEnter * CmdAlias jro RefactorRedo
  au VimEnter * CmdAlias jrop RefactorRedoPeek
  au VimEnter * CmdAlias jdstart JavaDebugStart
  au VimEnter * CmdAlias jdstop JavaDebugStop
  au VimEnter * CmdAlias jdb JavaDebugBreakpointToggle
  au VimEnter * CmdAlias jdbl JavaDebugBreakpointsList
  au VimEnter * CmdAlias jdbr JavaDebugBreakpointRemove
  au VimEnter * CmdAlias jds JavaDebugStatus
  au VimEnter * CmdAlias jdsa JavaDebugThreadSuspendAll
  au VimEnter * CmdAlias jdra JavaDebugThreadResumeAll
  au VimEnter * CmdAlias bm Bookmark
augroup END
