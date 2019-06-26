{if $lastSearch}
&nbsp;<a href="{$lastSearch|escape}#record{$recordDriver->getPermanentId()|escape:"url"}">{translate text="Catalog Search Results"}</a> <span class="divider">&raquo;</span>
{/if}
{if $pageTitleShort}
	<em>{$pageTitleShort}</em> <span class="divider">&raquo;</span>
{/if}
{if !empty($recordCount)}
	{translate text="Showing"}
	<b>{$recordStart}</b> - <b>{$recordEnd}</b>
	{translate text='of'} <b>{$recordCount}</b>
{/if}